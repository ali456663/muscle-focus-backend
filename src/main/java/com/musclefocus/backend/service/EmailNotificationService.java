package com.musclefocus.backend.service;

import com.musclefocus.backend.model.Lead;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

@Service
public class EmailNotificationService {

    @Value("${google.script.url}")
    private String googleScriptUrl;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();

    public void sendEmailNotification(Lead lead) {
        if (googleScriptUrl == null || googleScriptUrl.trim().isEmpty() || googleScriptUrl.contains("AKfycb...")) {
            System.out.println("Google Script URL is not configured or is placeholder. Skipping email notification.");
            return;
        }

        try {
            // Build JSON payload
            String jsonPayload = String.format(
                "{\"fullName\":\"%s\",\"gender\":\"%s\",\"age\":%d,\"city\":\"%s\",\"email\":\"%s\",\"phoneNumber\":\"%s\",\"trainingWish\":\"%s\",\"message\":\"%s\",\"paymentStatus\":\"%s\",\"amountPaid\":%s}",
                escapeJson(lead.getFullName()),
                escapeJson(lead.getGender()),
                lead.getAge(),
                escapeJson(lead.getCity()),
                escapeJson(lead.getEmail()),
                escapeJson(lead.getPhoneNumber()),
                escapeJson(lead.getTrainingWish()),
                escapeJson(lead.getMessage() != null ? lead.getMessage() : ""),
                escapeJson(lead.getPaymentStatus()),
                lead.getAmountPaid() != null ? lead.getAmountPaid().toString() : "null"
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(googleScriptUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            // Send asynchronously so it doesn't block the user's thread
            CompletableFuture<HttpResponse<String>> responseFuture = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());
            
            responseFuture.thenAccept(response -> {
                if (response.statusCode() == 200 || response.statusCode() == 302) {
                    System.out.println("Email notification sent successfully to Google Script.");
                } else {
                    System.err.println("Failed to send email notification. Status code: " + response.statusCode() + ", Response: " + response.body());
                }
            }).exceptionally(ex -> {
                System.err.println("Error sending email notification to Google Script: " + ex.getMessage());
                ex.printStackTrace();
                return null;
            });

        } catch (Exception e) {
            System.err.println("Error building email notification request: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\b", "\\b")
                  .replace("\f", "\\f")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}
