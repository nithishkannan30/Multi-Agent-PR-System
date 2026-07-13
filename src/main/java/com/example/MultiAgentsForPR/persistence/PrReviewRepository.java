package com.example.MultiAgentsForPR.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PrReviewRepository extends JpaRepository<PrReviewEntity, Long> {
}