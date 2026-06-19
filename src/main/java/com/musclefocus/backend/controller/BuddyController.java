package com.musclefocus.backend.controller;

import com.musclefocus.backend.model.Buddy;
import com.musclefocus.backend.repository.BuddyRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/buddies")
public class BuddyController {

    private final BuddyRepository buddyRepository;

    public BuddyController(BuddyRepository buddyRepository) {
        this.buddyRepository = buddyRepository;
    }

    // Public endpoint: Submit training buddy request
    @PostMapping
    public ResponseEntity<Buddy> createBuddy(@RequestBody Buddy buddy) {
        buddy.setId(null);
        if (buddy.getStatus() == null) {
            buddy.setStatus("APPROVED");
        }
        Buddy savedBuddy = buddyRepository.save(buddy);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedBuddy);
    }

    // Public endpoint: List active/approved training buddies
    @GetMapping
    public ResponseEntity<List<Buddy>> getActiveBuddies() {
        List<Buddy> approvedBuddies = buddyRepository.findByStatusOrderByCreatedAtDesc("APPROVED");
        return ResponseEntity.ok(approvedBuddies);
    }

    // Protected admin endpoint: List all buddies (including archived ones)
    @GetMapping("/admin-list")
    public ResponseEntity<List<Buddy>> getAllBuddies() {
        List<Buddy> allBuddies = buddyRepository.findAllByOrderByCreatedAtDesc();
        return ResponseEntity.ok(allBuddies);
    }

    // Protected admin endpoint: Update buddy status
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateBuddyStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String newStatus = body.get("status");
        if (newStatus == null || newStatus.isEmpty()) {
            return ResponseEntity.badRequest().body("Status saknas");
        }

        Optional<Buddy> buddyOptional = buddyRepository.findById(id);
        if (buddyOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Buddy buddy = buddyOptional.get();
        buddy.setStatus(newStatus.toUpperCase());
        Buddy updatedBuddy = buddyRepository.save(buddy);

        return ResponseEntity.ok(updatedBuddy);
    }

    // Protected admin endpoint: Delete a training buddy request
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteBuddy(@PathVariable Long id) {
        if (!buddyRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        buddyRepository.deleteById(id);
        return ResponseEntity.ok().body(Map.of("message", "Träningskompis har tagits bort"));
    }
}
