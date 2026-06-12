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
@Table(name = "certificates")
public class Certificate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String serialNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CertificateType type;

    @Column(nullable = false)
    private String subjectDN;

    @Column(nullable = false)
    private String issuerDN;

    @Column(nullable = false)
    private LocalDateTime validFrom;

    @Column(nullable = false)
    private LocalDateTime validUntil;

    @Column(nullable = false)
    private String algorithm;

    private Integer keySize;

    private String keyUsage;

    private String extendedKeyUsage;

    @Column(columnDefinition = "TEXT")
    private String publicKey;

    // Putanja do keystore fajla i alias unutar njega
    @Column(nullable = false)
    private String keystorePath;

    @Column(nullable = false)
    private String keystoreAlias;

    // Lozinka za keystore - hardkodovana vrednost, bez enkripcije (po specifikaciji za samostalne)
    @Column(nullable = false)
    private String keystorePassword;

    @Column(nullable = false)
    private boolean revoked = false;

    private LocalDateTime revokedAt;

    @Enumerated(EnumType.STRING)
    private RevocationReason revocationReason;

    // Referenca na issuer sertifikat, null za ROOT
    @ManyToOne
    @JoinColumn(name = "issuer_certificate_id")
    private Certificate issuerCertificate;

    // Email korisnika koji poseduje/je zatrazio sertifikat (za kontrolu pristupa)
    private String ownerEmail;

    // Organizacija (lanac) kojoj sertifikat pripada - CA korisnik pristupa samo svojoj
    private String organization;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    // PEM format za preuzimanje
    @Column(columnDefinition = "TEXT")
    private String certificatePem;
}