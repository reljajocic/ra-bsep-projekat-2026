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
        String ip = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");

        JwtResponse response = authService.login(email, password, ip, userAgent);
        return ResponseEntity.ok(response);
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
}