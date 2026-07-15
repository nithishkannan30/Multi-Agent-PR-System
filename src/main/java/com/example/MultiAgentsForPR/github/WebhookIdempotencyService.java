package com.example.MultiAgentsForPR.github;

import com.example.MultiAgentsForPR.persistence.ProcessedWebhookEntity;
import com.example.MultiAgentsForPR.persistence.ProcessedWebhookRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class WebhookIdempotencyService {

    private final ProcessedWebhookRepository repository;

    public WebhookIdempotencyService(ProcessedWebhookRepository repository) {
        this.repository = repository;
    }

    /**
     * Attempts to claim this delivery ID as "being processed."
     * Returns true if this is the first time we've seen it (safe to proceed).
     * Returns false if it's a duplicate (already processed or currently being processed).
     */
    public boolean tryClaim(String deliveryId) {
        if (deliveryId == null) return true; // no delivery ID available, can't dedupe, allow through

        try {
            repository.save(new ProcessedWebhookEntity(deliveryId));
            return true;
        } catch (DataIntegrityViolationException e) {
            // Unique constraint violation = someone already claimed this delivery ID
            return false;
        }
    }
}