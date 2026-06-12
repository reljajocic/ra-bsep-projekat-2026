package com.example.bsep.service;

import com.example.bsep.model.Certificate;
import com.example.bsep.model.CertificateTemplate;
import com.example.bsep.model.Role;
import com.example.bsep.model.User;
import com.example.bsep.repository.CertificateRepository;
import com.example.bsep.repository.CertificateTemplateRepository;
import com.example.bsep.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Service
@RequiredArgsConstructor
public class TemplateService {

    private static final Logger log = LoggerFactory.getLogger(TemplateService.class);

    private final CertificateTemplateRepository templateRepository;
    private final CertificateRepository certificateRepository;
    private final UserRepository userRepository;
    private final InputValidator validator;

    private User requireUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Korisnik ne postoji"));
    }

    public CertificateTemplate create(String name, Long issuerCertificateId, String cnRegex,
                                      String sanRegex, Integer ttlDays, String keyUsage,
                                      String extendedKeyUsage, String email) {
        User user = requireUser(email);
        validator.validateRequired(name, "Naziv šablona");
        validator.validateSafeText(name, "Naziv šablona");

        Certificate issuer = certificateRepository.findById(issuerCertificateId)
                .orElseThrow(() -> new RuntimeException("Issuer sertifikat ne postoji"));

        // CA korisnik moze da pravi sablone samo za svoju organizaciju (svoj lanac), nad INTERMEDIATE-om
        if (user.getRole() == Role.CA_USER) {
            if (issuer.getType() == com.example.bsep.model.CertificateType.ROOT) {
                throw new RuntimeException("CA korisnik ne može da koristi ROOT kao izdavaoca šablona");
            }
            if (user.getOrganization() == null
                    || !user.getOrganization().equalsIgnoreCase(issuer.getOrganization())) {
                throw new RuntimeException("Možete kreirati šablone samo za svoju organizaciju");
            }
        }

        validateRegex(cnRegex, "CN");
        validateRegex(sanRegex, "SAN");

        CertificateTemplate template = CertificateTemplate.builder()
                .name(validator.sanitize(name))
                .issuerCertificateId(issuerCertificateId)
                .organization(issuer.getOrganization())
                .cnRegex(emptyToNull(cnRegex))
                .sanRegex(emptyToNull(sanRegex))
                .ttlDays(ttlDays)
                .keyUsage(emptyToNull(keyUsage))
                .extendedKeyUsage(emptyToNull(extendedKeyUsage))
                .createdBy(email)
                .createdAt(LocalDateTime.now())
                .build();

        template = templateRepository.save(template);
        log.info("Šablon kreiran: id={}, naziv={}, organizacija={}", template.getId(), name, issuer.getOrganization());
        return template;
    }

    public List<CertificateTemplate> getForUser(String email) {
        User user = requireUser(email);
        if (user.getRole() == Role.ADMIN) {
            return templateRepository.findAll();
        }
        if (user.getOrganization() == null) {
            return List.of();
        }
        return templateRepository.findByOrganizationIgnoreCase(user.getOrganization());
    }

    public CertificateTemplate getForUserById(Long id, String email) {
        CertificateTemplate t = templateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Šablon ne postoji"));
        User user = requireUser(email);
        if (user.getRole() != Role.ADMIN
                && (user.getOrganization() == null
                    || !user.getOrganization().equalsIgnoreCase(t.getOrganization()))) {
            throw new RuntimeException("Nemate pristup ovom šablonu");
        }
        return t;
    }

    public void delete(Long id, String email) {
        CertificateTemplate t = getForUserById(id, email);
        templateRepository.delete(t);
        log.info("Šablon obrisan: id={}", id);
    }

    private void validateRegex(String regex, String field) {
        if (regex == null || regex.isBlank()) return;
        try {
            Pattern.compile(regex);
        } catch (PatternSyntaxException e) {
            throw new RuntimeException("Neispravan regularni izraz za " + field);
        }
    }

    private String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
