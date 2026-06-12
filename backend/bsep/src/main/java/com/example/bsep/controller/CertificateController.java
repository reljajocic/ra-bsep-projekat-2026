package com.example.bsep.controller;

import com.example.bsep.model.Certificate;
import com.example.bsep.model.CertificateType;
import com.example.bsep.service.CertificateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/certificates")
@RequiredArgsConstructor
public class CertificateController {

    private final CertificateService certificateService;

    @PostMapping
    public ResponseEntity<Certificate> issueCertificate(@RequestBody Map<String, Object> body,
                                                        @AuthenticationPrincipal String email) throws Exception {
        String subjectCN = (String) body.get("subjectCN");
        String subjectO = (String) body.get("subjectO");
        String subjectOU = (String) body.get("subjectOU");
        String subjectC = (String) body.get("subjectC");
        CertificateType type = CertificateType.valueOf((String) body.get("certificateType"));
        Long issuerCertificateId = body.get("issuerCertificateId") != null
                ? Long.valueOf(body.get("issuerCertificateId").toString()) : null;
        int validityDays = Integer.parseInt(body.get("validityDays").toString());
        String keyUsage = (String) body.get("keyUsage");
        String extendedKeyUsage = (String) body.get("extendedKeyUsage");
        String csrPem = (String) body.get("csrPem");
        Long templateId = body.get("templateId") != null
                ? Long.valueOf(body.get("templateId").toString()) : null;
        String subjectAltNames = (String) body.get("subjectAltNames");

        Certificate cert = certificateService.issueCertificate(
                subjectCN, subjectO, subjectOU, subjectC,
                type, issuerCertificateId, validityDays,
                keyUsage, extendedKeyUsage, csrPem, email,
                templateId, subjectAltNames
        );

        return ResponseEntity.ok(cert);
    }

    @GetMapping
    public ResponseEntity<List<Certificate>> getAllCertificates(@AuthenticationPrincipal String email) {
        return ResponseEntity.ok(certificateService.getCertificatesForUser(email));
    }

    @GetMapping("/issuers")
    public ResponseEntity<List<Certificate>> getAvailableIssuers(@AuthenticationPrincipal String email) {
        return ResponseEntity.ok(certificateService.getAvailableIssuers(email));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Certificate> getCertificate(@PathVariable Long id,
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(certificateService.getCertificateForUser(id, email));
    }

    @PostMapping("/{id}/revoke")
    public ResponseEntity<Void> revokeCertificate(@PathVariable Long id,
                                                  @RequestBody Map<String, String> body,
            @AuthenticationPrincipal String email) {
        String reason = body.getOrDefault("reason", "UNSPECIFIED");
        certificateService.revokeCertificate(id, reason, email);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleException(RuntimeException e) {
        String msg = e.getMessage();
        return ResponseEntity.status(403).body(Map.of("message", msg != null ? msg : "Greška"));
    }
}