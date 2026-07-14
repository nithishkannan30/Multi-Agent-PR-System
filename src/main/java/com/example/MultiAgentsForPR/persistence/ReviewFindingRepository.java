package com.example.MultiAgentsForPR.persistence;

import com.example.MultiAgentsForPR.model.Severity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewFindingRepository extends JpaRepository<ReviewFindingEntity, Long> {
    List<ReviewFindingEntity> findBySeverity(Severity severity);
    List<ReviewFindingEntity> findByAgentNameAndSeverity(String agentName, Severity severity);
}