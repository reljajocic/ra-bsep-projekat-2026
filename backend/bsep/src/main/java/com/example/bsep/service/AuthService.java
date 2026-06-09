package com.example.bsep.service;

import com.example.bsep.model.Role;
import com.example.bsep.model.SessionToken;
import com.example.bsep.model.User;
import com.example.bsep.repository.SessionTokenRepository;
import com.example.bsep.repository.UserRepository;
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

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final SessionTokenRepository sessionTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

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

    public JwtResponse login(String email, String password, String ipAddress, String userAgent) {
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

        String token = jwtUtil.generateToken(email);
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
        return new JwtResponse(token, email, user.getRole().name());
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
}