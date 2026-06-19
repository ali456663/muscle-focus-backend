package com.musclefocus.backend.controller;

import com.musclefocus.backend.model.Lead;
import com.musclefocus.backend.repository.LeadRepository;
import com.musclefocus.backend.service.EmailNotificationService;
import com.musclefocus.backend.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/leads")
public class LeadController {

    private final LeadRepository leadRepository;
    private final PaymentService paymentService;
    private final EmailNotificationService emailNotificationService;

    public LeadController(LeadRepository leadRepository, 
                          PaymentService paymentService, 
                          EmailNotificationService emailNotificationService) {
        this.leadRepository = leadRepository;
        this.paymentService = paymentService;
        this.emailNotificationService = emailNotificationService;
    }

    // Public endpoint: Submit application
    @PostMapping
    public ResponseEntity<?> createLead(@RequestBody Lead lead) {
        // Ensure ID is not set manually to prevent overwrites
        lead.setId(null);
        
        if (lead.getPayNow() != null && lead.getPayNow()) {
            lead.setPaymentStatus("PENDING_PAYMENT");
            Lead savedLead = leadRepository.save(lead);
            try {
                com.stripe.model.checkout.Session session = paymentService.createCheckoutSession(savedLead);
                savedLead.setStripeSessionId(session.getId());
                leadRepository.save(savedLead);
                
                return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "status", "PENDING_PAYMENT",
                    "leadId", savedLead.getId(),
                    "stripeCheckoutUrl", session.getUrl()
                ));
            } catch (Exception e) {
                e.printStackTrace();
                // Clean up and delete lead on stripe creation failure
                leadRepository.delete(savedLead);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("message", "Kunde inte skapa Stripe-betalningssession: " + e.getMessage()));
            }
        } else {
            lead.setPaymentStatus("NOT_REQUIRED");
            Lead savedLead = leadRepository.save(lead);
            
            // Send email notification to Ali for free consultation signup
            emailNotificationService.sendEmailNotification(savedLead);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(savedLead);
        }
    }

    // Protected admin endpoint: List all leads
    @GetMapping
    public ResponseEntity<List<Lead>> getAllLeads(@RequestParam(required = false) String status) {
        List<Lead> leads;
        if (status != null && !status.isEmpty()) {
            leads = leadRepository.findByStatusOrderByCreatedAtDesc(status);
        } else {
            leads = leadRepository.findAllByOrderByCreatedAtDesc();
        }
        return ResponseEntity.ok(leads);
    }

    // Protected admin endpoint: Update lead status
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateLeadStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String newStatus = body.get("status");
        if (newStatus == null || newStatus.isEmpty()) {
            return ResponseEntity.badRequest().body("Status saknas");
        }

        Optional<Lead> leadOptional = leadRepository.findById(id);
        if (leadOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Lead lead = leadOptional.get();
        lead.setStatus(newStatus.toUpperCase());
        Lead updatedLead = leadRepository.save(lead);

        return ResponseEntity.ok(updatedLead);
    }

    // Protected admin endpoint: Delete a lead
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteLead(@PathVariable Long id) {
        if (!leadRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        leadRepository.deleteById(id);
        return ResponseEntity.ok().body(Map.of("message", "Ansökan har tagits bort"));
    }
}
