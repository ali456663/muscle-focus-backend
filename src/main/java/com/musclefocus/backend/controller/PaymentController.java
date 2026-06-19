package com.musclefocus.backend.controller;

import com.musclefocus.backend.model.Lead;
import com.musclefocus.backend.repository.LeadRepository;
import com.musclefocus.backend.service.EmailNotificationService;
import com.musclefocus.backend.service.PaymentService;
import com.stripe.model.checkout.Session;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    private final PaymentService paymentService;
    private final LeadRepository leadRepository;
    private final EmailNotificationService emailNotificationService;

    public PaymentController(PaymentService paymentService, 
                             LeadRepository leadRepository, 
                             EmailNotificationService emailNotificationService) {
        this.paymentService = paymentService;
        this.leadRepository = leadRepository;
        this.emailNotificationService = emailNotificationService;
    }

    @GetMapping("/verify")
    public ResponseEntity<?> verifyPayment(@RequestParam("session_id") String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Session ID saknas"));
        }

        try {
            Session session = paymentService.retrieveSession(sessionId);
            
            if ("paid".equals(session.getPaymentStatus())) {
                String leadIdStr = session.getMetadata().get("lead_id");
                
                if (leadIdStr == null || leadIdStr.isEmpty()) {
                    return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Lead ID saknas i session-metadata"));
                }
                
                Long leadId = Long.parseLong(leadIdStr);
                Optional<Lead> leadOptional = leadRepository.findById(leadId);
                
                if (leadOptional.isEmpty()) {
                    return ResponseEntity.notFound().build();
                }
                
                Lead lead = leadOptional.get();
                
                // Only update and trigger notifications if not already marked paid
                if (!"PAID".equals(lead.getPaymentStatus())) {
                    lead.setPaymentStatus("PAID");
                    lead.setStripeSessionId(sessionId);
                    lead.setAmountPaid(session.getAmountTotal() / 100.0);
                    lead.setStatus("NEW"); // Make sure it shows up as new in admin dashboard
                    leadRepository.save(lead);
                    
                    // Trigger email notification to Ali
                    emailNotificationService.sendEmailNotification(lead);
                }
                
                return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "fullName", lead.getFullName(),
                    "trainingWish", lead.getTrainingWish(),
                    "amountPaid", lead.getAmountPaid()
                ));
            } else {
                return ResponseEntity.ok(Map.of("status", "pending", "message", "Betalning har inte genomförts än"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("status", "error", "message", "Kunde inte verifiera betalning: " + e.getMessage()));
        }
    }
}
