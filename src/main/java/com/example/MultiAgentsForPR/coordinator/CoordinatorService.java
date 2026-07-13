package com.example.MultiAgentsForPR.coordinator;

import com.example.MultiAgentsForPR.agents.requirements.RequirementsAgentService;
import com.example.MultiAgentsForPR.agents.security.SecurityAgentService;
import com.example.MultiAgentsForPR.agents.style.StyleAgentService;
import com.example.MultiAgentsForPR.model.*;
import com.example.MultiAgentsForPR.persistence.PrReviewEntity;
import com.example.MultiAgentsForPR.persistence.PrReviewRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class CoordinatorService {

    private final StyleAgentService styleAgentService;
    private final SecurityAgentService securityAgentService;
    private final RequirementsAgentService requirementsAgentService;
    private final PrReviewRepository prReviewRepository;
    private final ObjectMapper objectMapper;

    public CoordinatorService(StyleAgentService styleAgentService,
                              SecurityAgentService securityAgentService,
                              RequirementsAgentService requirementsAgentService,
                              PrReviewRepository prReviewRepository,
                              ObjectMapper objectMapper) {
        this.styleAgentService = styleAgentService;
        this.securityAgentService = securityAgentService;
        this.requirementsAgentService = requirementsAgentService;
        this.prReviewRepository = prReviewRepository;
        this.objectMapper = objectMapper;
    }

    public PrReviewResult review(String diff, String prDescription) {

        CompletableFuture<List<ReviewFinding>> styleFuture =
                CompletableFuture.supplyAsync(() -> styleAgentService.reviewDiff(diff));

        CompletableFuture<List<ReviewFinding>> securityFuture =
                CompletableFuture.supplyAsync(() -> securityAgentService.reviewDiff(diff));

        CompletableFuture<List<ReviewFinding>> requirementsFuture =
                CompletableFuture.supplyAsync(() -> requirementsAgentService.review(diff, prDescription));

        CompletableFuture.allOf(styleFuture, securityFuture, requirementsFuture).join();

        List<ReviewFinding> allFindings = new java.util.ArrayList<>();
        allFindings.addAll(styleFuture.join());
        allFindings.addAll(securityFuture.join());
        allFindings.addAll(requirementsFuture.join());

        Verdict verdict = decideVerdict(allFindings);
        String summary = buildSummary(allFindings, verdict);

        // Persist the review run
        try {
            String findingsJson = objectMapper.writeValueAsString(allFindings);
            PrReviewEntity entity = new PrReviewEntity(diff, prDescription, verdict.name(), findingsJson, summary);
            prReviewRepository.save(entity);
        } catch (Exception e) {
            // Don't let a persistence failure break the actual review response
            System.err.println("Failed to save review: " + e.getMessage());
        }

        return new PrReviewResult(verdict, allFindings, summary);
    }

    private Verdict decideVerdict(List<ReviewFinding> findings) {
        boolean hasCritical = findings.stream().anyMatch(f -> f.severity() == Severity.CRITICAL);
        boolean hasHigh = findings.stream().anyMatch(f -> f.severity() == Severity.HIGH);

        if (hasCritical || hasHigh) {
            return Verdict.REQUEST_CHANGES;
        } else if (!findings.isEmpty()) {
            return Verdict.COMMENT;
        } else {
            return Verdict.APPROVE;
        }
    }

    private String buildSummary(List<ReviewFinding> findings, Verdict verdict) {
        if (findings.isEmpty()) {
            return "No issues found. This PR looks good to merge.";
        }

        String breakdown = findings.stream()
                .collect(Collectors.groupingBy(ReviewFinding::agentName, Collectors.counting()))
                .entrySet().stream()
                .map(e -> e.getKey() + ": " + e.getValue() + " issue(s)")
                .collect(Collectors.joining(", "));

        return String.format("Verdict: %s. Found %d total issue(s) — %s.",
                verdict, findings.size(), breakdown);
    }
}