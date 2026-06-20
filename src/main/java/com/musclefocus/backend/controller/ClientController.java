package com.musclefocus.backend.controller;

import com.musclefocus.backend.model.Lead;
import com.musclefocus.backend.model.User;
import com.musclefocus.backend.repository.LeadRepository;
import com.musclefocus.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/client")
public class ClientController {

    private final UserRepository userRepository;
    private final LeadRepository leadRepository;

    public ClientController(UserRepository userRepository, LeadRepository leadRepository) {
        this.userRepository = userRepository;
        this.leadRepository = leadRepository;
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile() {
        String email = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Användare hittades inte");
        }

        User user = userOpt.get();
        Map<String, Object> profile = new HashMap<>();
        profile.put("fullName", user.getFullName());
        profile.put("email", user.getEmail());
        profile.put("phoneNumber", user.getPhoneNumber());
        profile.put("createdAt", user.getCreatedAt());

        return ResponseEntity.ok(profile);
    }

    @GetMapping("/history")
    public ResponseEntity<?> getHistory() {
        String email = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        List<Lead> leads = leadRepository.findByEmailOrderByCreatedAtDesc(email);
        return ResponseEntity.ok(leads);
    }
}
