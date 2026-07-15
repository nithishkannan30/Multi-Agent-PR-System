package com.example.MultiAgentsForPR.coordinator;

import com.example.MultiAgentsForPR.agents.requirements.RequirementsAgentService;
import com.example.MultiAgentsForPR.agents.security.SecurityAgentService;
import com.example.MultiAgentsForPR.agents.style.StyleAgentService;
import com.example.MultiAgentsForPR.metrics.ReviewMetrics;
import com.example.MultiAgentsForPR.model.*;
import com.example.MultiAgentsForPR.persistence.PrReviewEntity;
import com.example.MultiAgentsForPR.persistence.PrReviewRepository;
import com.example.MultiAgentsForPR.persistence.ReviewFindingEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class CoordinatorService {

    private static final Logger log = LoggerFactory.getLogger(CoordinatorService.class);
    private static final long AGENT_TIMEOUT_SECONDS = 20;

    private final StyleAgentService styleAgentService;
    private final SecurityAgentService securityAgentService;
    private final RequirementsAgentService requirementsAgentService;
    private final PrReviewRepository prReviewRepository;
    private final ObjectMapper objectMapper;
    private final Executor aiTaskExecutor;

    // Add field:
    private final ReviewMetrics metrics;

    // Add to constructor:
    public CoordinatorService(StyleAgentService styleAgentService,
                              SecurityAgentService securityAgentService,
                              RequirementsAgentService requirementsAgentService,
                              PrReviewRepository prReviewRepository,
                              ObjectMapper objectMapper,
                              @Qualifier("aiTaskExecutor") Executor aiTaskExecutor,
                              ReviewMetrics metrics) {
        this.styleAgentService = styleAgentService;
        this.securityAgentService = securityAgentService;
        this.requirementsAgentService = requirementsAgentService;
        this.prReviewRepository = prReviewRepository;
        this.objectMapper = objectMapper;
        this.aiTaskExecutor = aiTaskExecutor;
        this.metrics = metrics;
    }

    public PrReviewResult review(String diff, String prDescription, String owner, String repo, PrReviewMetadata metadata) {
        long startTime = System.currentTimeMillis();
        log.info("Starting PR review - diff length: {} chars", diff.length());

        CompletableFuture<List<ReviewFinding>> styleFuture = withTimeout(
                timed("StyleAgent", CompletableFuture.supplyAsync(() -> styleAgentService.reviewDiff(diff), aiTaskExecutor)),
                "StyleAgent");

        CompletableFuture<List<ReviewFinding>> securityFuture = withTimeout(
                timed("SecurityAgent", CompletableFuture.supplyAsync(() -> securityAgentService.reviewDiff(diff), aiTaskExecutor)),
                "SecurityAgent");

        CompletableFuture<List<ReviewFinding>> requirementsFuture = withTimeout(
                timed("RequirementsAgent", CompletableFuture.supplyAsync(() -> requirementsAgentService.review(diff, prDescription, owner, repo), aiTaskExecutor)),
                "RequirementsAgent");
        CompletableFuture.allOf(styleFuture, securityFuture, requirementsFuture).join();

        List<ReviewFinding> allFindings = new ArrayList<>();
        allFindings.addAll(styleFuture.join());
        allFindings.addAll(securityFuture.join());
        allFindings.addAll(requirementsFuture.join());

        Verdict verdict = decideVerdict(allFindings);
        String summary = buildSummary(allFindings, verdict);

        long duration = System.currentTimeMillis() - startTime;

        try {
            PrReviewEntity entity = new PrReviewEntity();
            entity.setOwner(owner);
            entity.setRepo(repo);
            entity.setPrNumber(metadata.prNumber());
            entity.setCommitSha(metadata.commitSha());
            entity.setBranch(metadata.branch());
            entity.setAuthor(metadata.author());
            entity.setPrDescription(prDescription);
            entity.setDiffUrl(metadata.diffUrl());
            entity.setVerdict(verdict.name());
            entity.setSummary(summary);
            entity.setDurationMs(duration);
            entity.setCreatedAt(java.time.LocalDateTime.now());

            List<ReviewFindingEntity> findingEntities = allFindings.stream()
                    .map(f -> new ReviewFindingEntity(f.agentName(), f.severity(), f.file(), f.line(), f.message()))
                    .collect(Collectors.toList());
            entity.setFindings(findingEntities);

            prReviewRepository.save(entity);
        } catch (Exception e) {
            System.err.println("Failed to save review: " + e.getMessage());
        }

        log.info("PR review completed in {}ms - verdict: {}, total findings: {}",
                duration, verdict, allFindings.size());
        metrics.recordReviewDuration(duration, verdict.name());
        metrics.incrementReviewsByVerdict(verdict.name());
        return new PrReviewResult(verdict, allFindings, summary);
    }

    /**
     * Wraps a future with a timeout. If the agent doesn't respond in time,
     * logs it and degrades gracefully to an empty list instead of blocking
     * the whole review indefinitely.
     */
    private CompletableFuture<List<ReviewFinding>> timed(String agentName, CompletableFuture<List<ReviewFinding>> future) {
        long start = System.currentTimeMillis();
        return future.whenComplete((result, ex) -> {
            metrics.recordAgentDuration(agentName, System.currentTimeMillis() - start);
            if (ex != null) {
                metrics.incrementAgentFailure(agentName);
            }
        });
    }

    private CompletableFuture<List<ReviewFinding>> withTimeout(CompletableFuture<List<ReviewFinding>> future, String agentName) {
        return future
                .orTimeout(AGENT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .exceptionally(ex -> {
                    log.warn("{} did not complete within {}s - returning empty result. Cause: {}",
                            agentName, AGENT_TIMEOUT_SECONDS, ex.getMessage());
                    return Collections.emptyList();
                });
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