package com.example.bsep.controller;

import com.example.bsep.model.PasswordEntry;
import com.example.bsep.service.PasswordManagerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/passwords")
@RequiredArgsConstructor
public class PasswordManagerController {

    private final PasswordManagerService passwordManagerService;

    @PostMapping
    public ResponseEntity<PasswordEntry> createEntry(@RequestBody Map<String, String> body,
                                                     @AuthenticationPrincipal String email) {
        String siteName = body.get("siteName");
        String username = body.get("username");
        String encryptedPassword = body.get("encryptedPassword");

        PasswordEntry entry = passwordManagerService.createEntry(siteName, username, encryptedPassword, email);
        return ResponseEntity.ok(entry);
    }

    @GetMapping
    public ResponseEntity<List<PasswordEntry>> getAllEntries() {
        return ResponseEntity.ok(passwordManagerService.getAllEntries());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEntry(@PathVariable Long id) {
        passwordManagerService.deleteEntry(id);
        return ResponseEntity.noContent().build();
    }
}