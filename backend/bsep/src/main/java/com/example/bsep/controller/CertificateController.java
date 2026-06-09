package com.example.bsep.controller;

import com.example.bsep.model.Certificate;
import com.example.bsep.model.CertificateType;
import com.example.bsep.service.CertificateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/certificates")
@RequiredArgsConstructor
public class CertificateController {

    private final CertificateService certificateService;

    @PostMapping
    public ResponseEntity<Certificate> issueCertificate(@RequestBody Map<String, Object> body) throws Exception {
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

        Certificate cert = certificateService.issueCertificate(
                subjectCN, subjectO, subjectOU, subjectC,
                type, issuerCertificateId, validityDays,
                keyUsage, extendedKeyUsage, csrPem
        );

        return ResponseEntity.ok(cert);
    }

    @GetMapping
    public ResponseEntity<List<Certificate>> getAllCertificates() {
        return ResponseEntity.ok(certificateService.getAllCertificates());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Certificate> getCertificate(@PathVariable Long id) {
        return ResponseEntity.ok(certificateService.getCertificateById(id));
    }

    @PostMapping("/{id}/revoke")
    public ResponseEntity<Void> revokeCertificate(@PathVariable Long id,
                                                  @RequestBody Map<String, String> body) {
        String reason = body.getOrDefault("reason", "UNSPECIFIED");
        certificateService.revokeCertificate(id, reason);
        return ResponseEntity.noContent().build();
    }
}