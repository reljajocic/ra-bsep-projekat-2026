package com.example.bsep.repository;

import com.example.bsep.model.Certificate;
import com.example.bsep.model.CertificateType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CertificateRepository extends JpaRepository<Certificate, Long> {

    List<Certificate> findByType(CertificateType type);

    List<Certificate> findByRevokedFalse();

    List<Certificate> findByOwnerEmail(String ownerEmail);

    List<Certificate> findByOrganizationIgnoreCase(String organization);
}