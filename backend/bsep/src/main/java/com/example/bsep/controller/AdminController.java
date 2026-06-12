package com.example.bsep.controller;

import com.example.bsep.model.User;
import com.example.bsep.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AuthService authService;

    @PostMapping("/ca-users")
    public ResponseEntity<Map<String, String>> createCaUser(@RequestBody Map<String, String> body) {
        authService.createCaUser(
                body.get("email"),
                body.get("firstName"),
                body.get("lastName"),
                body.get("organization")
        );
        return ResponseEntity.ok(Map.of("message",
                "CA korisnik kreiran. Privremena lozinka je poslata na email."));
    }

    @GetMapping("/ca-users")
    public ResponseEntity<List<Map<String, Object>>> getCaUsers() {
        List<Map<String, Object>> result = authService.getCaUsers().stream()
                .map(u -> Map.<String, Object>of(
                        "id", u.getId(),
                        "email", u.getEmail(),
                        "firstName", u.getFirstName(),
                        "lastName", u.getLastName(),
                        "organization", u.getOrganization() != null ? u.getOrganization() : "",
                        "mustChangePassword", u.isMustChangePassword()
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handle(RuntimeException e) {
        String msg = e.getMessage();
        return ResponseEntity.status(400).body(Map.of("error", msg != null ? msg : "Greška"));
    }
}
