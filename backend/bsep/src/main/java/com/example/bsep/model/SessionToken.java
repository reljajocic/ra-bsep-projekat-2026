package com.example.bsep.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "session_tokens")
public class SessionToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // JWT ID - identifikuje token bez čuvanja celog tokena
    @Column(nullable = false, unique = true)
    private String jti;

    @Column(nullable = false)
    private String userEmail;

    private String ipAddress;
    private String userAgent;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime lastSeen;

    @Column(nullable = false)
    private boolean revoked = false;
}