package com.example.bsep.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// Sablon definise ekstenzije i politiku za izdavanje sertifikata pod odredjenim CA issuerom
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "certificate_templates")
public class CertificateTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    // CA sertifikat koji izdaje na osnovu ovog sablona
    @Column(nullable = false)
    private Long issuerCertificateId;

    // Organizacija (lanac) kojoj sablon pripada - radi kontrole pristupa
    private String organization;

    // Regularni izraz za validaciju Common Name novog sertifikata
    private String cnRegex;

    // Regularni izraz za validaciju Subject Alternative Names
    private String sanRegex;

    // Maksimalno trajanje (TTL) u danima
    private Integer ttlDays;

    // Podrazumevane vrednosti ekstenzija
    private String keyUsage;

    private String extendedKeyUsage;

    @Column(nullable = false)
    private String createdBy;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
