package com.example.bsep.controller;

import com.example.bsep.model.SessionToken;
import com.example.bsep.security.JwtResponse;
import com.example.bsep.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login(@RequestBody Map<String, String> body,
                                             HttpServletRequest request) {
        String email = body.get("email");
        String password = body.get("password");
        String totpCode = body.get("totpCode");
        String ip = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");

        JwtResponse response = authService.login(email, password, totpCode, ip, userAgent);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@RequestBody Map<String, String> body) {
        authService.register(
                body.get("email"),
                body.get("password"),
                body.get("firstName"),
                body.get("lastName"),
                body.get("organization")
        );
        return ResponseEntity.ok(Map.of("message",
                "Registracija uspešna. Proverite email za aktivacioni link."));
    }

    @GetMapping("/activate")
    public ResponseEntity<Map<String, String>> activate(@RequestParam String token) {
        authService.activate(token);
        return ResponseEntity.ok(Map.of("message", "Nalog je uspešno aktiviran. Možete se prijaviti."));
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(@RequestBody Map<String, String> body,
                                                              @AuthenticationPrincipal String email) {
        authService.changePassword(email, body.get("oldPassword"), body.get("newPassword"));
        return ResponseEntity.ok(Map.of("message", "Lozinka uspešno promenjena."));
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<SessionToken>> getSessions(@AuthenticationPrincipal String email) {
        return ResponseEntity.ok(authService.getActiveSessions(email));
    }

    @DeleteMapping("/sessions/{jti}")
    public ResponseEntity<Void> revokeSession(@PathVariable String jti,
                                              @AuthenticationPrincipal String email) {
        authService.revokeSession(jti, email);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/2fa/setup")
    public ResponseEntity<Map<String, String>> setupTotp(@AuthenticationPrincipal String email) throws Exception {
        String qrCode = authService.setupTotp(email);
        return ResponseEntity.ok(Map.of("qrCode", qrCode));
    }

    @PostMapping("/2fa/confirm")
    public ResponseEntity<Void> confirmTotp(@RequestBody Map<String, String> body,
                                            @AuthenticationPrincipal String email) {
        authService.confirmTotp(email, body.get("code"));
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleAuthException(RuntimeException e) {
        String msg = e.getMessage();
        return ResponseEntity.status(403).body(Map.of("error", msg != null ? msg : "Authentication failed"));
    }

    @GetMapping("/2fa/status")
    public ResponseEntity<Map<String, Boolean>> totpStatus(@AuthenticationPrincipal String email) {
        boolean enabled = authService.isTotpEnabled(email);
        return ResponseEntity.ok(Map.of("enabled", enabled));
    }
}