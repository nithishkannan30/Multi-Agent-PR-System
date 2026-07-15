package com.example.MultiAgentsForPR.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedWebhookRepository extends JpaRepository<ProcessedWebhookEntity, Long> {
    boolean existsByDeliveryId(String deliveryId);
}