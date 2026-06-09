package com.example.bsep.service;

import com.example.bsep.model.Certificate;
import com.example.bsep.model.CertificateType;
import com.example.bsep.model.RevocationReason;
import com.example.bsep.repository.CertificateRepository;
import lombok.RequiredArgsConstructor;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaContentVerifierProviderBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CertificateService {

    private final CertificateRepository certificateRepository;
    private final KeyStoreService keyStoreService;

    private static final Logger log = LoggerFactory.getLogger(CertificateService.class);

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Transactional
    public Certificate issueCertificate(String subjectCN, String subjectO, String subjectOU,
                                        String subjectC, CertificateType type,
                                        Long issuerCertificateId, int validityDays,
                                        String keyUsage, String extendedKeyUsage,
                                        String csrPem) throws Exception {

        log.info("Issuing certificate: type={}, CN={}", type, subjectCN);

        // Validacija tipa
        if (type == CertificateType.ROOT && issuerCertificateId != null) {
            throw new IllegalArgumentException("ROOT certificate cannot have an issuer");
        }
        if (type != CertificateType.ROOT && issuerCertificateId == null) {
            throw new IllegalArgumentException(type + " certificate must have an issuer");
        }

        Certificate issuerEntity = null;
        PrivateKey issuerPrivateKey = null;
        X509Certificate issuerX509 = null;

        if (issuerCertificateId != null) {
            issuerEntity = certificateRepository.findById(issuerCertificateId)
                    .orElseThrow(() -> new RuntimeException("Issuer certificate not found"));

            validateIssuer(issuerEntity);

            issuerPrivateKey = keyStoreService.loadPrivateKey(
                    issuerEntity.getKeystorePath(),
                    issuerEntity.getKeystoreAlias()
            );
            issuerX509 = keyStoreService.loadCertificate(
                    issuerEntity.getKeystorePath(),
                    issuerEntity.getKeystoreAlias()
            );
        }

        KeyPair keyPair;
        X500Name subject;
        boolean csrBased = false;

        // END_ENTITY moze koristiti CSR
        if (type == CertificateType.END_ENTITY && csrPem != null && !csrPem.isBlank()) {
            PKCS10CertificationRequest csr = parseAndValidateCsr(csrPem);
            PublicKey publicKey = new JcaPEMKeyConverter()
                    .setProvider("BC")
                    .getPublicKey(csr.getSubjectPublicKeyInfo());
            keyPair = new KeyPair(publicKey, null);
            subject = csr.getSubject();
            csrBased = true;
            log.info("Using CSR for END_ENTITY certificate, subject={}", subject);
        } else {
            keyPair = generateKeyPair();
            subject = buildX500Name(subjectCN, subjectO, subjectOU, subjectC);
        }

        // Validacija da END_ENTITY ne prelazi period vazenja issuera
        if (issuerEntity != null) {
            LocalDateTime maxValid = issuerEntity.getValidUntil();
            LocalDateTime requestedValid = LocalDateTime.now().plusDays(validityDays);
            if (requestedValid.isAfter(maxValid)) {
                throw new IllegalArgumentException(
                        "Certificate validity cannot exceed issuer validity: " + maxValid
                );
            }
        }

        X509Certificate x509 = buildCertificate(
                subject, keyPair, issuerPrivateKey, issuerX509,
                type, issuerEntity, validityDays, keyUsage, extendedKeyUsage
        );

        String alias = "cert_" + UUID.randomUUID();
        String keystorePath = keyStoreService.getKeystoreBasePath() + "/" + alias + ".p12";

        if (csrBased) {
            keyStoreService.createTrustedCertificateKeyStore(keystorePath, alias, x509);
        } else {
            keyStoreService.createKeyStore(keystorePath, alias, keyPair.getPrivate(), x509);
        }

        Certificate cert = Certificate.builder()
                .serialNumber(x509.getSerialNumber().toString())
                .type(type)
                .subjectDN(x509.getSubjectX500Principal().getName())
                .issuerDN(x509.getIssuerX500Principal().getName())
                .validFrom(LocalDateTime.ofInstant(x509.getNotBefore().toInstant(), ZoneId.systemDefault()))
                .validUntil(LocalDateTime.ofInstant(x509.getNotAfter().toInstant(), ZoneId.systemDefault()))
                .algorithm("RSA")
                .keySize(2048)
                .keyUsage(keyUsage)
                .extendedKeyUsage(extendedKeyUsage)
                .publicKey(java.util.Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()))
                .keystorePath(keystorePath)
                .keystoreAlias(alias)
                .keystorePassword(keyStoreService.getKeystorePassword())
                .revoked(false)
                .issuerCertificate(issuerEntity)
                .createdAt(LocalDateTime.now())
                .certificatePem(toPem(x509))
                .build();

        cert = certificateRepository.save(cert);
        log.info("Certificate issued: id={}, serialNumber={}, type={}", cert.getId(), cert.getSerialNumber(), cert.getType());
        return cert;
    }

    public List<Certificate> getAllCertificates() {
        return certificateRepository.findAll();
    }

    public Certificate getCertificateById(Long id) {
        return certificateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Certificate not found: " + id));
    }

    @Transactional
    public void revokeCertificate(Long id, String reason) {
        Certificate cert = getCertificateById(id);

        if (cert.isRevoked()) {
            throw new IllegalArgumentException("Certificate is already revoked");
        }

        cert.setRevoked(true);
        cert.setRevokedAt(LocalDateTime.now());
        cert.setRevocationReason(RevocationReason.valueOf(reason));
        certificateRepository.save(cert);

        log.info("Certificate revoked: id={}, reason={}", id, reason);
    }

    // --- Private helpers ---

    private KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA", "BC");
        gen.initialize(2048, new SecureRandom());
        return gen.generateKeyPair();
    }

    private X500Name buildX500Name(String cn, String o, String ou, String c) {
        X500NameBuilder builder = new X500NameBuilder(BCStyle.INSTANCE);
        builder.addRDN(BCStyle.CN, cn);
        if (o != null && !o.isBlank()) builder.addRDN(BCStyle.O, o);
        if (ou != null && !ou.isBlank()) builder.addRDN(BCStyle.OU, ou);
        if (c != null && !c.isBlank()) builder.addRDN(BCStyle.C, c);
        return builder.build();
    }

    private X509Certificate buildCertificate(X500Name subject, KeyPair keyPair,
                                             PrivateKey issuerKey, X509Certificate issuerX509,
                                             CertificateType type, Certificate issuerEntity,
                                             int validityDays, String keyUsage,
                                             String extendedKeyUsage) throws Exception {

        X500Name issuerDN = (type == CertificateType.ROOT)
                ? subject
                : new X500Name(issuerX509.getSubjectX500Principal().getName());

        BigInteger serial = new BigInteger(160, new SecureRandom());
        Date notBefore = new Date();
        Date notAfter = Date.from(LocalDateTime.now().plusDays(validityDays)
                .atZone(ZoneId.systemDefault()).toInstant());

        X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                issuerDN, serial, notBefore, notAfter, subject, keyPair.getPublic()
        );

        // BasicConstraints - CA ili end-entity
        boolean isCA = type == CertificateType.ROOT || type == CertificateType.INTERMEDIATE;
        certBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(isCA));

        // SubjectKeyIdentifier
        certBuilder.addExtension(Extension.subjectKeyIdentifier, false,
                new SubjectKeyIdentifier(keyPair.getPublic().getEncoded()));

        // KeyUsage
        if (keyUsage != null && !keyUsage.isBlank()) {
            certBuilder.addExtension(Extension.keyUsage, true,
                    new KeyUsage(parseKeyUsage(keyUsage)));
        }

        // ExtendedKeyUsage
        if (extendedKeyUsage != null && !extendedKeyUsage.isBlank()) {
            certBuilder.addExtension(Extension.extendedKeyUsage, false,
                    new ExtendedKeyUsage(parseExtendedKeyUsage(extendedKeyUsage)));
        }

        PrivateKey signingKey = (type == CertificateType.ROOT) ? keyPair.getPrivate() : issuerKey;
        ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSAEncryption")
                .setProvider("BC")
                .build(signingKey);

        X509CertificateHolder holder = certBuilder.build(signer);
        return new JcaX509CertificateConverter().setProvider("BC").getCertificate(holder);
    }

    private PKCS10CertificationRequest parseAndValidateCsr(String csrPem) throws Exception {
        try (PEMParser parser = new PEMParser(new StringReader(csrPem))) {
            Object obj = parser.readObject();
            if (!(obj instanceof PKCS10CertificationRequest)) {
                throw new IllegalArgumentException("Invalid CSR format");
            }
            PKCS10CertificationRequest csr = (PKCS10CertificationRequest) obj;
            boolean valid = csr.isSignatureValid(
                    new JcaContentVerifierProviderBuilder().setProvider("BC")
                            .build(csr.getSubjectPublicKeyInfo())
            );
            if (!valid) {
                throw new IllegalArgumentException("CSR signature is invalid");
            }
            return csr;
        }
    }

    private void validateIssuer(Certificate issuer) {
        if (issuer.isRevoked()) {
            throw new IllegalArgumentException("Issuer certificate is revoked");
        }
        if (LocalDateTime.now().isAfter(issuer.getValidUntil())) {
            throw new IllegalArgumentException("Issuer certificate has expired");
        }
        if (issuer.getType() == CertificateType.END_ENTITY) {
            throw new IllegalArgumentException("END_ENTITY certificate cannot be used as issuer");
        }
    }

    private int parseKeyUsage(String keyUsageStr) {
        int usage = 0;
        for (String part : keyUsageStr.split(",")) {
            switch (part.trim().toLowerCase()) {
                case "digitalsignature" -> usage |= KeyUsage.digitalSignature;
                case "keyencipherment" -> usage |= KeyUsage.keyEncipherment;
                case "keycertsign" -> usage |= KeyUsage.keyCertSign;
                case "crlsign" -> usage |= KeyUsage.cRLSign;
                case "dataencipherment" -> usage |= KeyUsage.dataEncipherment;
            }
        }
        return usage;
    }

    private KeyPurposeId[] parseExtendedKeyUsage(String ekuStr) {
        List<KeyPurposeId> purposes = new ArrayList<>();
        for (String part : ekuStr.split(",")) {
            switch (part.trim().toLowerCase()) {
                case "serverauth" -> purposes.add(KeyPurposeId.id_kp_serverAuth);
                case "clientauth" -> purposes.add(KeyPurposeId.id_kp_clientAuth);
                case "codesigning" -> purposes.add(KeyPurposeId.id_kp_codeSigning);
                case "emailprotection" -> purposes.add(KeyPurposeId.id_kp_emailProtection);
            }
        }
        return purposes.toArray(new KeyPurposeId[0]);
    }

    private String toPem(X509Certificate cert) throws Exception {
        StringWriter sw = new StringWriter();
        try (JcaPEMWriter pw = new JcaPEMWriter(sw)) {
            pw.writeObject(cert);
        }
        return sw.toString();
    }
}