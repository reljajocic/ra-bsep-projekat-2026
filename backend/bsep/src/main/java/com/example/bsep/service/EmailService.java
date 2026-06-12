package com.example.bsep.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${app.frontend-url:https://localhost:5173}")
    private String frontendUrl;

    public void sendActivationEmail(String to, String token) {
        String link = frontendUrl + "/activate?token=" + token;
        String subject = "Aktivacija BSEP naloga";
        String text = "Dobrodošli!\n\nAktivirajte svoj nalog klikom na link (važi 24h, jednokratan):\n"
                + link + "\n\nUkoliko niste Vi pokrenuli registraciju, ignorišite ovaj mejl.";
        send(to, subject, text, link);
    }

    public void sendCaUserCredentials(String to, String password) {
        String subject = "BSEP - kreiran CA nalog";
        String text = "Kreiran Vam je CA nalog na BSEP PKI sistemu.\n\n"
                + "Email: " + to + "\n"
                + "Privremena lozinka: " + password + "\n\n"
                + "Pri prvoj prijavi bićete obavezni da promenite lozinku.\nLink: " + frontendUrl + "/login";
        send(to, subject, text, frontendUrl + "/login");
    }

    public void sendPasswordResetEmail(String to, String token) {
        String link = frontendUrl + "/reset-password?token=" + token;
        String subject = "Oporavak BSEP naloga";
        String text = "Za promenu lozinke kliknite na link (važi 24h, jednokratan):\n" + link;
        send(to, subject, text, link);
    }

    // Ako SMTP nije konfigurisan (nema username-a), link se samo loguje da bi dev/demo radio
    private void send(String to, String subject, String text, String link) {
        if (mailUsername == null || mailUsername.isBlank()) {
            log.warn("SMTP nije konfigurisan - email za {} se NE salje (demo rezim).\n"
                    + "--- SADRZAJ EMAILA ---\nSubject: {}\n{}\n--- KRAJ ---", to, subject, text);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            message.setFrom(mailUsername);
            mailSender.send(message);
            log.info("Email poslat: to={}, subject={}", to, subject);
        } catch (Exception e) {
            log.error("Slanje emaila nije uspelo: to={}, greska={}", to, e.getMessage());
            log.warn("Link (fallback): {}", link);
        }
    }
}
