package com.example.bsep.controller;

import com.example.bsep.model.CertificateTemplate;
import com.example.bsep.service.TemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateService templateService;

    @PostMapping
    public ResponseEntity<CertificateTemplate> create(@RequestBody Map<String, Object> body,
                                                      @AuthenticationPrincipal String email) {
        String name = (String) body.get("name");
        Long issuerCertificateId = Long.valueOf(body.get("issuerCertificateId").toString());
        String cnRegex = (String) body.get("cnRegex");
        String sanRegex = (String) body.get("sanRegex");
        Integer ttlDays = body.get("ttlDays") != null ? Integer.valueOf(body.get("ttlDays").toString()) : null;
        String keyUsage = (String) body.get("keyUsage");
        String extendedKeyUsage = (String) body.get("extendedKeyUsage");

        CertificateTemplate t = templateService.create(name, issuerCertificateId, cnRegex,
                sanRegex, ttlDays, keyUsage, extendedKeyUsage, email);
        return ResponseEntity.ok(t);
    }

    @GetMapping
    public ResponseEntity<List<CertificateTemplate>> getAll(@AuthenticationPrincipal String email) {
        return ResponseEntity.ok(templateService.getForUser(email));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal String email) {
        templateService.delete(id, email);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handle(RuntimeException e) {
        String msg = e.getMessage();
        return ResponseEntity.status(400).body(Map.of("message", msg != null ? msg : "Greška"));
    }
}
