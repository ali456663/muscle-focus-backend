package com.musclefocus.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "leads")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String gender;

    @Column(nullable = false)
    private Integer age;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String phoneNumber;

    @Column(nullable = false)
    private String trainingWish;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false)
    private String status; // e.g., "NEW", "CONTACTED", "COMPLETED", "ARCHIVED"

    @Column(nullable = false)
    private String paymentStatus; // e.g., "NOT_REQUIRED", "PENDING_PAYMENT", "PAID"

    @Column
    private String stripeSessionId;

    @Column
    private Double amountPaid;

    @Column
    private String priceOption; // e.g., "regular", "discounted"

    @Transient
    private Boolean payNow;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = "NEW";
        }
        if (this.paymentStatus == null) {
            this.paymentStatus = "NOT_REQUIRED";
        }
    }
}
