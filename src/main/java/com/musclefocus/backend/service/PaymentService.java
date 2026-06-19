package com.musclefocus.backend.service;

import com.musclefocus.backend.model.Lead;
import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Service
public class PaymentService {

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @PostConstruct
    public void init() {
        // Initialize Stripe SDK with API key
        Stripe.apiKey = stripeApiKey;
    }

    public Session createCheckoutSession(Lead lead) throws Exception {
        Map<String, Object> priceAndName = getPriceAndNameForWish(lead.getTrainingWish(), lead.getPriceOption());
        long amountInCents = (long) priceAndName.get("priceInCents");
        String productName = (String) priceAndName.get("productName");

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(frontendUrl + "/payment/success?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(frontendUrl + "/payment/cancel?lead_id=" + lead.getId())
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                // Optionally add Klarna/other payment methods if desired
                .setCustomerEmail(lead.getEmail())
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency("sek")
                                                .setUnitAmount(amountInCents)
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName(productName)
                                                                .setDescription(lead.getTrainingWish())
                                                                .build()
                                                )
                                                .build()
                                )
                                .build()
                )
                .putMetadata("lead_id", lead.getId().toString())
                .build();

        return Session.create(params);
    }

    public Session retrieveSession(String sessionId) throws Exception {
        return Session.retrieve(sessionId);
    }

    public Map<String, Object> getPriceAndNameForWish(String trainingWish, String priceOption) {
        String wishLower = trainingWish.toLowerCase();
        long priceInCents = 112900L; // Default fallback price
        String productName = "Muscle & Focus PT Tjänst";
        boolean isDiscounted = "discounted".equalsIgnoreCase(priceOption);

        if (wishLower.contains("sommar") || wishLower.contains("summer")) {
            priceInCents = 149900L;
            productName = "Sommarkampanj - Muscle & Focus";
        } else if (wishLower.contains("black friday")) {
            priceInCents = 129900L;
            productName = "Black Friday Transformation";
        } else if (wishLower.contains("jul") || wishLower.contains("christ")) {
            priceInCents = 119900L;
            productName = "Julerbjudande - Muscle & Focus";
        } else if (wishLower.contains("komplett") || wishLower.contains("pt online-26") || (wishLower.contains("pt online") && wishLower.contains("26"))) {
            priceInCents = 579000L;
            productName = "PT online Komplett (26 Veckor)";
        } else if (wishLower.contains("next level")) {
            priceInCents = 579000L;
            productName = "Next Level Coaching (26 Veckor)";
        } else if (wishLower.contains("body reboot")) {
            priceInCents = 579000L;
            productName = "Body Reboot (26 Veckor)";
        } else if (wishLower.contains("livsstil") || wishLower.contains("lifestyle")) {
            priceInCents = 389000L;
            productName = "PT online Livsstilsstarten (16 Veckor)";
        } else if (wishLower.contains("glute") || wishLower.contains("leg")) {
            priceInCents = 389000L;
            productName = "PT online Glute & Leg Specialist";
        } else if (wishLower.contains("fokus") || wishLower.contains("focus")) {
            priceInCents = 299000L;
            productName = "PT online Fokus (12 Veckor)";
        } else if (wishLower.contains("hälsa") || wishLower.contains("health") || wishLower.contains("styrka")) {
            priceInCents = 199900L;
            productName = "PT online Styrka & Hälsa (8 Veckor)";
        } else if (wishLower.contains("projekt") || wishLower.contains("kickstart") || wishLower.contains("4 veckor")) {
            if (isDiscounted) {
                priceInCents = 85600L;
                productName = "PT online Projekt (4 Veckor) - Student/Senior";
            } else {
                priceInCents = 112900L;
                productName = "PT online Projekt (4 Veckor)";
            }
        } else if (wishLower.contains("kostschema") || wishLower.contains("nutrition")) {
            priceInCents = 69900L;
            productName = "Avancerat Kostschema";
        } else if (wishLower.contains("teknik") || wishLower.contains("tech")) {
            if (isDiscounted) {
                priceInCents = 34000L;
                productName = "Teknikträning (60 Min) - Student/Senior";
            } else {
                priceInCents = 49900L;
                productName = "Teknikträning (60) Minuter";
            }
        } else if (wishLower.contains("individuell") || wishLower.contains("individual")) {
            priceInCents = 44900L;
            productName = "Individuell PT-träning 60 min";
        }

        Map<String, Object> result = new HashMap<>();
        result.put("priceInCents", priceInCents);
        result.put("productName", productName);
        return result;
    }
}
