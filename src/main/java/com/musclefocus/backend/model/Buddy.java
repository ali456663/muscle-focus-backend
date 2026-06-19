package com.musclefocus.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "buddies")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Buddy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private Integer age;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String gym;

    @Column(nullable = false)
    private String contactInfo; // e.g. email, phone, or instagram handler

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false)
    private String status; // "APPROVED" or "ARCHIVED"

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = "APPROVED"; // auto-approved to make it active instantly
        }
    }
}
