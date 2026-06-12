package com.example.bsep.service;

import com.example.bsep.model.Certificate;
import com.example.bsep.model.CertificateType;
import com.example.bsep.model.RevocationReason;
import com.example.bsep.model.Role;
import com.example.bsep.model.User;
import com.example.bsep.repository.CertificateRepository;
import com.example.bsep.repository.UserRepository;
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
    private final UserRepository userRepository;
    private final com.example.bsep.repository.CertificateTemplateRepository templateRepository;

    private static final Logger log = LoggerFactory.getLogger(CertificateService.class);
    private final InputValidator validator;

    private User requireUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Korisnik ne postoji"));
    }

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Transactional
    public Certificate issueCertificate(String subjectCN, String subjectO, String subjectOU,
                                        String subjectC, CertificateType type,
                                        Long issuerCertificateId, int validityDays,
                                        String keyUsage, String extendedKeyUsage,
                                        String csrPem, String requesterEmail,
                                        Long templateId, String subjectAltNames) throws Exception {

        User requester = requireUser(requesterEmail);
        Role role = requester.getRole();

        // Primena sablona (opciono) - popunjava podrazumevane ekstenzije i namece politiku
        com.example.bsep.model.CertificateTemplate template = null;
        if (templateId != null) {
            template = templateRepository.findById(templateId)
                    .orElseThrow(() -> new IllegalArgumentException("Šablon ne postoji"));
            // Sablon mora odgovarati odabranom issueru
            if (issuerCertificateId == null || !template.getIssuerCertificateId().equals(issuerCertificateId)) {
                throw new IllegalArgumentException("Šablon ne odgovara odabranom CA izdavaocu");
            }
            // Podrazumevane ekstenzije ako korisnik nije uneo svoje
            if ((keyUsage == null || keyUsage.isBlank()) && template.getKeyUsage() != null) {
                keyUsage = template.getKeyUsage();
            }
            if ((extendedKeyUsage == null || extendedKeyUsage.isBlank()) && template.getExtendedKeyUsage() != null) {
                extendedKeyUsage = template.getExtendedKeyUsage();
            }
            // TTL - trajanje ne sme da prevazidje maksimum iz sablona
            if (template.getTtlDays() != null && validityDays > template.getTtlDays()) {
                throw new IllegalArgumentException(
                        "Trajanje prevazilazi maksimum iz šablona (" + template.getTtlDays() + " dana)");
            }
        }

        // Autorizacija po ulozi:
        // - ADMIN: sve
        // - CA_USER: INTERMEDIATE i END_ENTITY, iskljucivo u okviru svoje organizacije (lanca)
        // - USER: samo END_ENTITY i iskljucivo preko CSR-a (privatni kljuc ostaje kod korisnika)
        if (role == Role.USER) {
            if (type != CertificateType.END_ENTITY) {
                throw new IllegalArgumentException("Nemate ovlašćenje za izdavanje ovog tipa sertifikata");
            }
            if (csrPem == null || csrPem.isBlank()) {
                throw new IllegalArgumentException("Obavezan je upload CSR-a za izdavanje sertifikata");
            }
        } else if (role == Role.CA_USER) {
            if (type == CertificateType.ROOT) {
                throw new IllegalArgumentException("CA korisnik ne može da izda ROOT sertifikat");
            }
        }

        // Validacija unosa (preskace se za CSR jer podaci dolaze iz potpisanog zahteva)
        if (csrPem == null || csrPem.isBlank()) {
            validator.validateRequired(subjectCN, "Common Name");
            validator.validateSafeText(subjectCN, "Common Name");
            validator.validateSafeText(subjectO, "Organization");
            validator.validateSafeText(subjectOU, "Organizational Unit");
            validator.validateCountry(subjectC);
        }
        validator.validateValidityDays(validityDays);

        log.info("Issuing certificate: type={}, CN={}", type, subjectCN);

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

            // CA korisnik sme da koristi issuer samo iz svoje organizacije (svog lanca)
            if (role == Role.CA_USER) {
                if (issuerEntity.getType() == CertificateType.ROOT) {
                    throw new IllegalArgumentException(
                            "CA korisnik ne može da koristi ROOT kao izdavaoca (ROOT je rezervisan za administratora)");
                }
                String userOrg = requester.getOrganization();
                if (userOrg == null || !userOrg.equalsIgnoreCase(issuerEntity.getOrganization())) {
                    throw new IllegalArgumentException(
                            "Možete izdavati samo u okviru svoje organizacije (svog lanca)");
                }
            }

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

        // Validacija po sablonu: CN i SAN moraju da zadovolje regularne izraze iz sablona
        if (template != null) {
            String effectiveCN = extractCN(subject);
            if (template.getCnRegex() != null && effectiveCN != null
                    && !effectiveCN.matches(template.getCnRegex())) {
                throw new IllegalArgumentException(
                        "Common Name '" + effectiveCN + "' ne zadovoljava šablon (" + template.getCnRegex() + ")");
            }
            if (template.getSanRegex() != null && subjectAltNames != null && !subjectAltNames.isBlank()) {
                for (String san : subjectAltNames.split(",")) {
                    String s = san.trim();
                    if (!s.isEmpty() && !s.matches(template.getSanRegex())) {
                        throw new IllegalArgumentException(
                                "SAN '" + s + "' ne zadovoljava šablon (" + template.getSanRegex() + ")");
                    }
                }
            }
        }

        X509Certificate x509 = buildCertificate(
                subject, keyPair, issuerPrivateKey, issuerX509,
                type, issuerEntity, validityDays, keyUsage, extendedKeyUsage, subjectAltNames
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
                .ownerEmail(requesterEmail)
                .organization(issuerEntity != null ? issuerEntity.getOrganization() : subjectO)
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

    // ADMIN vidi sve, CA_USER samo svoj lanac (organizaciju), USER samo svoje sertifikate
    public List<Certificate> getCertificatesForUser(String email) {
        User user = requireUser(email);
        return switch (user.getRole()) {
            case ADMIN -> certificateRepository.findAll();
            case CA_USER -> user.getOrganization() == null ? List.of()
                    : certificateRepository.findByOrganizationIgnoreCase(user.getOrganization());
            case USER -> certificateRepository.findByOwnerEmail(email);
        };
    }

    private boolean canAccess(Certificate cert, User user) {
        return switch (user.getRole()) {
            case ADMIN -> true;
            case CA_USER -> user.getOrganization() != null
                    && user.getOrganization().equalsIgnoreCase(cert.getOrganization());
            case USER -> user.getEmail().equals(cert.getOwnerEmail());
        };
    }

    // CA sertifikati (ROOT/INTERMEDIATE) koji su validni i mogu da potpisuju - za odabir issuera.
    // CA korisnik vidi samo issuere iz svoje organizacije; ADMIN i USER vide sve dostupne CA.
    public List<Certificate> getAvailableIssuers(String email) {
        User user = requireUser(email);
        List<Certificate> result = new ArrayList<>();
        for (Certificate c : certificateRepository.findByRevokedFalse()) {
            if (c.getType() == CertificateType.END_ENTITY
                    || !LocalDateTime.now().isBefore(c.getValidUntil())) {
                continue;
            }
            if (user.getRole() == Role.CA_USER) {
                // CA korisnik sme da koristi samo INTERMEDIATE iz svoje organizacije; ROOT je adminov
                if (c.getType() == CertificateType.ROOT) {
                    continue;
                }
                if (user.getOrganization() == null
                        || !user.getOrganization().equalsIgnoreCase(c.getOrganization())) {
                    continue;
                }
            }
            result.add(c);
        }
        return result;
    }

    public Certificate getCertificateById(Long id) {
        return certificateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Certificate not found: " + id));
    }

    // Provera pristupa pojedinacnom sertifikatu
    public Certificate getCertificateForUser(Long id, String email) {
        Certificate cert = getCertificateById(id);
        if (!canAccess(cert, requireUser(email))) {
            throw new RuntimeException("Nemate pristup ovom sertifikatu");
        }
        return cert;
    }

    @Transactional
    public void revokeCertificate(Long id, String reason, String email) {
        Certificate cert = getCertificateById(id);

        if (!canAccess(cert, requireUser(email))) {
            throw new RuntimeException("Nemate ovlašćenje za povlačenje ovog sertifikata");
        }

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

    private String extractCN(X500Name name) {
        org.bouncycastle.asn1.x500.RDN[] rdns = name.getRDNs(BCStyle.CN);
        if (rdns.length == 0) return null;
        return org.bouncycastle.asn1.x500.style.IETFUtils.valueToString(rdns[0].getFirst().getValue());
    }

    private X509Certificate buildCertificate(X500Name subject, KeyPair keyPair,
                                             PrivateKey issuerKey, X509Certificate issuerX509,
                                             CertificateType type, Certificate issuerEntity,
                                             int validityDays, String keyUsage,
                                             String extendedKeyUsage, String subjectAltNames) throws Exception {

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

        // SubjectAlternativeName - DNS/IP/email iz unosa (comma-separated)
        if (subjectAltNames != null && !subjectAltNames.isBlank()) {
            List<GeneralName> names = new ArrayList<>();
            for (String raw : subjectAltNames.split(",")) {
                String s = raw.trim();
                if (s.isEmpty()) continue;
                int tag = s.contains("@") ? GeneralName.rfc822Name
                        : s.matches("^[0-9.]+$") ? GeneralName.iPAddress
                        : GeneralName.dNSName;
                names.add(new GeneralName(tag, s));
            }
            if (!names.isEmpty()) {
                certBuilder.addExtension(Extension.subjectAlternativeName, false,
                        new GeneralNames(names.toArray(new GeneralName[0])));
            }
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