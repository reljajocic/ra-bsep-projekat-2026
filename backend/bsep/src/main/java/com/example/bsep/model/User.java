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
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    private String firstName;
    private String lastName;
    private String organization;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    private boolean enabled;

    private String totpSecret;

    private boolean totpEnabled = false;

    // CA korisnik mora da promeni podrazumevanu lozinku pri prvom pristupu
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean mustChangePassword = false;
}