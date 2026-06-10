package com.example.bsep.service;

import dev.samstevens.totp.code.*;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.util.Utils;
import org.springframework.stereotype.Service;

@Service
public class TotpService {

    private final DefaultSecretGenerator secretGenerator = new DefaultSecretGenerator();
    private final SystemTimeProvider timeProvider = new SystemTimeProvider();

    // Generise nasumicni tajni kljuc za korisnika
    public String generateSecret() {
        return secretGenerator.generate();
    }

    // Pravi data URI sa QR kodom koji korisnik skenira u authenticator aplikaciji
    public String generateQrCodeImageUri(String secret, String email) throws QrGenerationException {
        QrData data = new QrData.Builder()
                .label(email)
                .secret(secret)
                .issuer("BSEP PKI")
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();

        ZxingPngQrGenerator generator = new ZxingPngQrGenerator();
        byte[] imageData = generator.generate(data);
        return Utils.getDataUriForImage(imageData, generator.getImageMimeType());
    }

    // Proverava da li je 6-cifreni kod koji je korisnik uneo ispravan
    public boolean verifyCode(String secret, String code) {
        CodeVerifier verifier = new DefaultCodeVerifier(
                new DefaultCodeGenerator(), timeProvider
        );
        return verifier.isValidCode(secret, code);
    }
}