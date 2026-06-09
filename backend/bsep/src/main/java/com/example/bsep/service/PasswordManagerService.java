package com.example.bsep.service;

import com.example.bsep.model.PasswordEntry;
import com.example.bsep.repository.PasswordEntryRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PasswordManagerService {

    private final PasswordEntryRepository passwordEntryRepository;

    private static final Logger log = LoggerFactory.getLogger(PasswordManagerService.class);

    // Lozinka dolazi vec enkriptovana sa frontenda (Web Crypto API, RSA javni kljuc)
    @Transactional
    public PasswordEntry createEntry(String siteName, String username,
                                     String encryptedPassword, String adminEmail) {

        log.info("Creating password entry: site={}, admin={}", siteName, adminEmail);

        PasswordEntry entry = PasswordEntry.builder()
                .siteName(siteName)
                .username(username)
                .encryptedPassword(encryptedPassword)
                .createdAt(LocalDateTime.now())
                .createdBy(adminEmail)
                .build();

        entry = passwordEntryRepository.save(entry);
        log.info("Password entry created: id={}, site={}", entry.getId(), entry.getSiteName());
        return entry;
    }

    public List<PasswordEntry> getAllEntries() {
        return passwordEntryRepository.findAll();
    }

    public PasswordEntry getEntryById(Long id) {
        return passwordEntryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Password entry not found: " + id));
    }

    @Transactional
    public void deleteEntry(Long id) {
        PasswordEntry entry = getEntryById(id);
        passwordEntryRepository.delete(entry);
        log.info("Password entry deleted: id={}, site={}", id, entry.getSiteName());
    }
}