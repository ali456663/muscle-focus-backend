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

    public AuthController(
            JwtTokenProvider tokenProvider,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            EmailNotificationService emailNotificationService,
            @Value("${musclefocus.admin.username}") String adminUsername,
            @Value("${musclefocus.admin.password}") String adminPassword) {
        this.tokenProvider = tokenProvider;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailNotificationService = emailNotificationService;
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

        // Create a mock Lead object to represent the password reset request
        Lead resetRequestLead = Lead.builder()
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .gender("Kvinna/Man")
                .age(30)
                .city("Password Reset")
                .trainingWish("LÖSENORDSÅTERSTÄLLNING")
                .message("Klienten " + user.getFullName() + " har klickat på 'Glömt lösenord' och begärt att få sitt lösenord återställt. Vänligen kontakta klienten på tel: " + user.getPhoneNumber() + " eller e-post: " + user.getEmail() + " för att hjälpa dem.")
                .status("NEW")
                .paymentStatus("NOT_REQUIRED")
                .amountPaid(0.0)
                .build();

        emailNotificationService.sendEmailNotification(resetRequestLead);

        return ResponseEntity.ok("En begäran om återställning har skickats till din tränare Ali. Vi kommer kontakta dig inom kort.");
    }
}
