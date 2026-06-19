package com.musclefocus.backend.controller;

import com.musclefocus.backend.dto.LoginRequest;
import com.musclefocus.backend.dto.LoginResponse;
import com.musclefocus.backend.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final JwtTokenProvider tokenProvider;
    private final String adminUsername;
    private final String adminPassword;

    public AuthController(
            JwtTokenProvider tokenProvider,
            @Value("${musclefocus.admin.username}") String adminUsername,
            @Value("${musclefocus.admin.password}") String adminPassword) {
        this.tokenProvider = tokenProvider;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        if (adminUsername.equals(loginRequest.getUsername()) && adminPassword.equals(loginRequest.getPassword())) {
            String token = tokenProvider.generateToken(adminUsername);
            return ResponseEntity.ok(new LoginResponse(token, adminUsername));
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body("Felaktigt användarnamn eller lösenord");
    }
}
