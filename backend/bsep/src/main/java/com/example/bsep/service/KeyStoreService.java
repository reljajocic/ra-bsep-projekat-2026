package com.example.bsep.service;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

@Service
public class KeyStoreService {

    private static final Logger log = LoggerFactory.getLogger(KeyStoreService.class);
    private static final String KEYSTORE_TYPE = "PKCS12";
    private static final String KEYSTORE_PASSWORD = "bsep2026";

    @Value("${keystore.base-path}")
    private String keystoreBasePath;

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    // Kreira keystore sa privatnim kljucem i sertifikatom (za ROOT i INTERMEDIATE)
    public void createKeyStore(String keystorePath, String alias,
                               PrivateKey privateKey, X509Certificate certificate) throws Exception {

        KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE, "BC");
        keyStore.load(null, null);

        Certificate[] chain = {certificate};
        keyStore.setKeyEntry(alias, privateKey, KEYSTORE_PASSWORD.toCharArray(), chain);

        try (FileOutputStream fos = new FileOutputStream(keystorePath)) {
            keyStore.store(fos, KEYSTORE_PASSWORD.toCharArray());
        }

        log.info("KeyStore created: path={}, alias={}", keystorePath, alias);
    }

    // Kreira keystore samo sa sertifikatom, bez privatnog kljuca (za END_ENTITY)
    public void createTrustedCertificateKeyStore(String keystorePath, String alias,
                                                 X509Certificate certificate) throws Exception {

        KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE, "BC");
        keyStore.load(null, null);

        keyStore.setCertificateEntry(alias, certificate);

        try (FileOutputStream fos = new FileOutputStream(keystorePath)) {
            keyStore.store(fos, KEYSTORE_PASSWORD.toCharArray());
        }

        log.info("Trusted certificate KeyStore created: path={}, alias={}", keystorePath, alias);
    }

    public PrivateKey loadPrivateKey(String keystorePath, String alias) throws Exception {
        KeyStore keyStore = loadKeyStore(keystorePath);
        Key key = keyStore.getKey(alias, KEYSTORE_PASSWORD.toCharArray());

        if (key instanceof PrivateKey) {
            return (PrivateKey) key;
        }

        throw new IllegalArgumentException("No private key found for alias: " + alias);
    }

    public X509Certificate loadCertificate(String keystorePath, String alias) throws Exception {
        KeyStore keyStore = loadKeyStore(keystorePath);
        Certificate cert = keyStore.getCertificate(alias);

        if (cert instanceof X509Certificate) {
            return (X509Certificate) cert;
        }

        throw new IllegalArgumentException("No certificate found for alias: " + alias);
    }

    private KeyStore loadKeyStore(String keystorePath) throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE, "BC");
        try (FileInputStream fis = new FileInputStream(keystorePath)) {
            keyStore.load(fis, KEYSTORE_PASSWORD.toCharArray());
        }
        return keyStore;
    }

    public String getKeystoreBasePath() {
        return keystoreBasePath;
    }

    public String getKeystorePassword() {
        return KEYSTORE_PASSWORD;
    }
}