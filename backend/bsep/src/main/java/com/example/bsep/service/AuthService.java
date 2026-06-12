package com.example.bsep.service;

import com.example.bsep.model.Role;
import com.example.bsep.model.SessionToken;
import com.example.bsep.model.TokenType;
import com.example.bsep.model.User;
import com.example.bsep.model.VerificationToken;
import com.example.bsep.repository.SessionTokenRepository;
import com.example.bsep.repository.UserRepository;
import com.example.bsep.repository.VerificationTokenRepository;
import com.example.bsep.security.JwtResponse;
import com.example.bsep.security.JwtUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final SessionTokenRepository sessionTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private final TotpService totpService;
    private final VerificationTokenRepository verificationTokenRepository;
    private final EmailService emailService;
    private final InputValidator validator;

    // Registracija obicnog korisnika - nalog je onemogucen dok se ne aktivira preko mejla
    public void register(String email, String password, String firstName,
                         String lastName, String organization) {
        validator.validateEmail(email);
        validator.validateRequired(firstName, "Ime");
        validator.validateRequired(lastName, "Prezime");
        validator.validateRequired(organization, "Organizacija");
        validator.validateSafeText(firstName, "Ime");
        validator.validateSafeText(lastName, "Prezime");
        validator.validateSafeText(organization, "Organizacija");
        validator.validatePassword(password);

        if (userRepository.existsByEmail(email)) {
            log.warn("Registracija odbijena - email vec postoji: email={}", email);
            throw new RuntimeException("Korisnik sa ovom email adresom već postoji");
        }

        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .firstName(validator.sanitize(firstName))
                .lastName(validator.sanitize(lastName))
                .organization(validator.sanitize(organization))
                .role(Role.USER)
                .enabled(false)
                .build();
        userRepository.save(user);

        String token = UUID.randomUUID().toString();
        VerificationToken vt = VerificationToken.builder()
                .token(token)
                .userEmail(email)
                .type(TokenType.ACTIVATION)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .used(false)
                .build();
        verificationTokenRepository.save(vt);

        emailService.sendActivationEmail(email, token);
        log.info("Korisnik registrovan (neaktiviran): email={}", email);
    }

    // Aktivacija naloga preko jednokratnog, vremenski ogranicenog tokena
    public void activate(String token) {
        VerificationToken vt = verificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Nevažeći aktivacioni link"));

        if (vt.getType() != TokenType.ACTIVATION || !vt.isValid()) {
            log.warn("Aktivacija neuspesna - token nevazeci/iskoriscen: token={}", token);
            throw new RuntimeException("Aktivacioni link je istekao ili je već iskorišćen");
        }

        User user = userRepository.findByEmail(vt.getUserEmail())
                .orElseThrow(() -> new RuntimeException("Korisnik ne postoji"));

        user.setEnabled(true);
        userRepository.save(user);

        vt.setUsed(true);
        verificationTokenRepository.save(vt);

        log.info("Nalog aktiviran: email={}", user.getEmail());
    }

    // Kreira inicijalnog admina ako ne postoji u bazi
    @PostConstruct
    public void createInitialAdmin() {
        if (!userRepository.existsByEmail("admin@bsep.com")) {
            User admin = User.builder()
                    .email("admin@bsep.com")
                    .password(passwordEncoder.encode("Admin1234!"))
                    .firstName("Admin")
                    .lastName("BSEP")
                    .role(Role.ADMIN)
                    .enabled(true)
                    .build();
            userRepository.save(admin);
            log.info("Initial admin created: admin@bsep.com");
        }
    }

    public JwtResponse login(String email, String password, String totpCode, String ipAddress, String userAgent) {
        log.info("Login attempt: email={}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Login failed - user not found: email={}", email);
                    return new RuntimeException("Invalid credentials");
                });

        if (!user.isEnabled()) {
            log.warn("Login failed - account disabled: email={}", email);
            throw new RuntimeException("Account is disabled");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            log.warn("Login failed - wrong password: email={}", email);
            throw new RuntimeException("Invalid credentials");
        }

        // Provera 2FA ako je ukljucen
        if (user.isTotpEnabled()) {
            if (totpCode == null || totpCode.isBlank()) {
                log.info("2FA required: email={}", email);
                throw new RuntimeException("2FA_REQUIRED");
            }
            if (!totpService.verifyCode(user.getTotpSecret(), totpCode)) {
                log.warn("Login failed - invalid 2FA code: email={}", email);
                throw new RuntimeException("Invalid 2FA code");
            }
        }

        String token = jwtUtil.generateToken(email, user.getRole().name());
        String jti = jwtUtil.extractJti(token);

        SessionToken sessionToken = SessionToken.builder()
                .jti(jti)
                .userEmail(email)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .createdAt(LocalDateTime.now())
                .lastSeen(LocalDateTime.now())
                .revoked(false)
                .build();

        sessionTokenRepository.save(sessionToken);

        log.info("Login successful: email={}", email);
        return new JwtResponse(token, email, user.getRole().name(), user.isMustChangePassword());
    }

    // Admin kreira CA korisnika - sistem generise nasumicnu lozinku i salje je na mejl
    public void createCaUser(String email, String firstName, String lastName, String organization) {
        validator.validateEmail(email);
        validator.validateRequired(firstName, "Ime");
        validator.validateRequired(lastName, "Prezime");
        validator.validateRequired(organization, "Organizacija");
        validator.validateSafeText(firstName, "Ime");
        validator.validateSafeText(lastName, "Prezime");
        validator.validateSafeText(organization, "Organizacija");

        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Korisnik sa ovom email adresom već postoji");
        }

        String generatedPassword = generateRandomPassword();

        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(generatedPassword))
                .firstName(validator.sanitize(firstName))
                .lastName(validator.sanitize(lastName))
                .organization(validator.sanitize(organization))
                .role(Role.CA_USER)
                .enabled(true)
                .mustChangePassword(true)
                .build();
        userRepository.save(user);

        emailService.sendCaUserCredentials(email, generatedPassword);
        log.info("CA korisnik kreiran: email={}, organizacija={}", email, organization);
    }

    // Promena lozinke - koristi se i za prvu obaveznu promenu CA korisnika
    public void changePassword(String email, String oldPassword, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Korisnik ne postoji"));

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            log.warn("Promena lozinke neuspesna - pogresna trenutna lozinka: email={}", email);
            throw new RuntimeException("Trenutna lozinka nije ispravna");
        }
        validator.validatePassword(newPassword);
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new RuntimeException("Nova lozinka mora biti različita od trenutne");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        userRepository.save(user);
        log.info("Lozinka promenjena: email={}", email);
    }

    public List<User> getCaUsers() {
        return userRepository.findByRole(Role.CA_USER);
    }

    // Nasumicna lozinka koja zadovoljava politiku (veliko/malo slovo, cifra, specijalni znak)
    private String generateRandomPassword() {
        String upper = "ABCDEFGHJKLMNPQRSTUVWXYZ";
        String lower = "abcdefghijkmnpqrstuvwxyz";
        String digits = "23456789";
        String special = "!@#$%&*";
        String all = upper + lower + digits + special;
        java.security.SecureRandom rnd = new java.security.SecureRandom();
        StringBuilder sb = new StringBuilder();
        sb.append(upper.charAt(rnd.nextInt(upper.length())));
        sb.append(lower.charAt(rnd.nextInt(lower.length())));
        sb.append(digits.charAt(rnd.nextInt(digits.length())));
        sb.append(special.charAt(rnd.nextInt(special.length())));
        for (int i = 0; i < 12; i++) {
            sb.append(all.charAt(rnd.nextInt(all.length())));
        }
        return sb.toString();
    }

    public List<SessionToken> getActiveSessions(String email) {
        return sessionTokenRepository.findByUserEmailAndRevokedFalse(email);
    }

    public void revokeSession(String jti, String email) {
        SessionToken token = sessionTokenRepository.findByJti(jti)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        if (!token.getUserEmail().equals(email)) {
            throw new RuntimeException("Unauthorized");
        }

        token.setRevoked(true);
        sessionTokenRepository.save(token);
        log.info("Session revoked: jti={}, email={}", jti, email);
    }

    // Pokrece setup 2FA - generise secret i vraca QR kod
    public String setupTotp(String email) throws Exception {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String secret = totpService.generateSecret();
        user.setTotpSecret(secret);
        userRepository.save(user);

        log.info("2FA setup initiated: email={}", email);
        return totpService.generateQrCodeImageUri(secret, email);
    }

    // Potvrdjuje setup - korisnik unese prvi kod da dokaze da je skenirao QR
    public void confirmTotp(String email, String code) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!totpService.verifyCode(user.getTotpSecret(), code)) {
            throw new RuntimeException("Invalid 2FA code");
        }

        user.setTotpEnabled(true);
        userRepository.save(user);
        log.info("2FA enabled: email={}", email);
    }

    public boolean isTotpEnabled(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return user.isTotpEnabled();
    }
}