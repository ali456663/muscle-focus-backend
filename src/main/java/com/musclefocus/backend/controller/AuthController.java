package com.musclefocus.backend.controller;

import com.musclefocus.backend.dto.LoginRequest;
import com.musclefocus.backend.dto.LoginResponse;
import com.musclefocus.backend.dto.RegisterRequest;
import com.musclefocus.backend.model.User;
import com.musclefocus.backend.model.Lead;
import com.musclefocus.backend.repository.UserRepository;
import com.musclefocus.backend.security.JwtTokenProvider;
import com.musclefocus.backend.service.EmailNotificationService;
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
    private final EmailNotificationService emailNotificationService;
    private final String adminUsername;
    private final String adminPassword;
    private final String frontendUrl;

    public AuthController(
            JwtTokenProvider tokenProvider,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            EmailNotificationService emailNotificationService,
            @Value("${musclefocus.admin.username}") String adminUsername,
            @Value("${musclefocus.admin.password}") String adminPassword,
            @Value("${app.frontend.url}") String frontendUrl) {
        this.tokenProvider = tokenProvider;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailNotificationService = emailNotificationService;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
        this.frontendUrl = frontendUrl;
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

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody java.util.Map<String, String> payload) {
        String email = payload.get("email");
        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("E-postadress krävs");
        }

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Denna e-postadress är inte registrerad.");
        }

        User user = userOpt.get();
        String token = java.util.UUID.randomUUID().toString();
        user.setResetToken(token);
        user.setResetTokenExpiry(java.time.LocalDateTime.now().plusHours(24));
        userRepository.save(user);

        String resetLink = frontendUrl + "/reset-password?token=" + token;

        // Create a mock Lead object to represent the password reset request
        Lead resetRequestLead = Lead.builder()
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .gender("Kvinna/Man")
                .age(30)
                .city("Password Reset")
                .trainingWish("LÖSENORDSÅTERSTÄLLNING")
                .message("Klienten " + user.getFullName() + " har begärt att få återställa sitt lösenord.\n\n"
                        + "För att kunden ska kunna välja sitt nya lösenord själv, vidarebefordra denna länk till kunden (via mejl, SMS eller WhatsApp):\n\n"
                        + resetLink + "\n\n"
                        + "Länken är giltig i 24 timmar.")
                .status("NEW")
                .paymentStatus("NOT_REQUIRED")
                .amountPaid(0.0)
                .build();

        emailNotificationService.sendEmailNotification(resetRequestLead);

        return ResponseEntity.ok("En begäran om återställning har skickats till din tränare Ali. Vi kommer kontakta dig inom kort.");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody java.util.Map<String, String> payload) {
        String token = payload.get("token");
        String password = payload.get("password");

        if (token == null || token.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Token och nytt lösenord krävs");
        }

        Optional<User> userOpt = userRepository.findByResetToken(token);
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Återställningskoden är ogiltig.");
        }

        User user = userOpt.get();
        if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(java.time.LocalDateTime.now())) {
            return ResponseEntity.badRequest().body("Återställningslänken har tyvärr gått ut (giltig i 24 timmar).");
        }

        user.setPassword(passwordEncoder.encode(password));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);

        return ResponseEntity.ok("Ditt lösenord har återställts. Du kan nu logga in med ditt nya lösenord.");
    }
}
