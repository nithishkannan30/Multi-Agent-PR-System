package com.example.MultiAgentsForPR.persistence;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "processed_webhooks", uniqueConstraints = {
        @UniqueConstraint(columnNames = "delivery_id")
})
public class ProcessedWebhookEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "delivery_id", nullable = false)
    private String deliveryId;

    private LocalDateTime processedAt;

    public ProcessedWebhookEntity() {}

    public ProcessedWebhookEntity(String deliveryId) {
        this.deliveryId = deliveryId;
        this.processedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getDeliveryId() { return deliveryId; }
    public LocalDateTime getProcessedAt() { return processedAt; }
}