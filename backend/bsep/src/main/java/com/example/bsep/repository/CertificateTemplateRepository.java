package com.example.bsep.repository;

import com.example.bsep.model.CertificateTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CertificateTemplateRepository extends JpaRepository<CertificateTemplate, Long> {

    List<CertificateTemplate> findByOrganizationIgnoreCase(String organization);
}
