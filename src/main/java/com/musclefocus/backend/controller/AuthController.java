package com.musclefocus.backend.controller;

import com.musclefocus.backend.dto.LoginRequest;
import com.musclefocus.backend.dto.LoginResponse;
import com.musclefocus.backend.dto.RegisterRequest;
import com.musclefocus.backend.model.User;
import com.musclefocus.backend.repository.UserRepository;
import com.musclefocus.backend.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminUsername;
    private final String adminPassword;

    public AuthController(
            JwtTokenProvider tokenProvider,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${musclefocus.admin.username}") String adminUsername,
            @Value("${musclefocus.admin.password}") String adminPassword) {
        this.tokenProvider = tokenProvider;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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

    @PostMapping("/register")
    public ResponseEntity<?> registerClient(@RequestBody RegisterRequest registerRequest) {
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            return ResponseEntity.badRequest().body("E-posten är redan registrerad");
        }

        User user = User.builder()
                .fullName(registerRequest.getFullName())
                .phoneNumber(registerRequest.getPhoneNumber())
                .email(registerRequest.getEmail())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .role("ROLE_CLIENT")
                .build();

        userRepository.save(user);

        String token = tokenProvider.generateToken(user.getEmail());
        return ResponseEntity.ok(new LoginResponse(token, user.getEmail()));
    }

    @PostMapping("/client-login")
    public ResponseEntity<?> clientLogin(@RequestBody LoginRequest loginRequest) {
        Optional<User> userOpt = userRepository.findByEmail(loginRequest.getUsername());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
                String token = tokenProvider.generateToken(user.getEmail());
                return ResponseEntity.ok(new LoginResponse(token, user.getEmail()));
            }
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body("Felaktig e-post eller lösenord");
    }
}
