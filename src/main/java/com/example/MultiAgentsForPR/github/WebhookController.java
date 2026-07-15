package com.example.MultiAgentsForPR.github;

import com.example.MultiAgentsForPR.coordinator.CoordinatorService;
import com.example.MultiAgentsForPR.metrics.ReviewMetrics;
import com.example.MultiAgentsForPR.model.PrReviewResult;
import com.example.MultiAgentsForPR.rag.RepoIndexingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
public class WebhookController {

    private final CoordinatorService coordinatorService;
    private final GitHubApiClient gitHubApiClient;
    private final ObjectMapper objectMapper;
    private final String webhookSecret;

    // Add this field + constructor param:
    private final RepoIndexingService repoIndexingService;

    // In constructor, add:
    private final WebhookIdempotencyService idempotencyService;

    // Add to constructor:
    private final ReviewMetrics metrics;

    public WebhookController(CoordinatorService coordinatorService,
                             GitHubApiClient gitHubApiClient,
                             ObjectMapper objectMapper,
                             RepoIndexingService repoIndexingService,
                             WebhookIdempotencyService idempotencyService,
                             @Value("${github.webhook.secret}") String webhookSecret,
                             ReviewMetrics metrics) {
        this.coordinatorService = coordinatorService;
        this.gitHubApiClient = gitHubApiClient;
        this.objectMapper = objectMapper;
        this.repoIndexingService = repoIndexingService;
        this.idempotencyService = idempotencyService;
        this.webhookSecret = webhookSecret;
        this.metrics=metrics;
    }

    @PostMapping("/webhooks/github")
    public String handleWebhook(@RequestBody String rawBody,
                                @RequestHeader("X-Hub-Signature-256") String signature,
                                @RequestHeader("X-GitHub-Event") String eventType,
                                @RequestHeader(value = "X-GitHub-Delivery", required = false) String deliveryId) throws Exception {

        if (!isSignatureValid(rawBody, signature)) {
            return "Invalid signature";
        }

        if (!idempotencyService.tryClaim(deliveryId)) {
            System.out.println("[IDEMPOTENCY] Duplicate delivery ignored: " + deliveryId);
            return "Duplicate delivery - already processed";
        }

        if ("push".equals(eventType)) {
            handlePushEvent(rawBody);
            return "Push processed - incremental index updated";
        }

        if (!"pull_request".equals(eventType)) {
            return "Ignored - event was " + eventType;
        }

        WebhookPayload payload = objectMapper.readValue(rawBody, WebhookPayload.class);

        if (!"opened".equals(payload.action()) && !"synchronize".equals(payload.action())) {
            return "Ignored - action was " + payload.action();
        }

        String owner = payload.repository().owner().login();
        String repo = payload.repository().name();
        int prNumber = payload.pull_request().number();
        String prDescription = payload.pull_request().body() != null ? payload.pull_request().body() : "";

        repoIndexingService.indexRepoIfNeeded(owner, repo);

        String diff = gitHubApiClient.getPrDiff(owner, repo, prNumber);

        com.example.MultiAgentsForPR.model.PrReviewMetadata metadata = new com.example.MultiAgentsForPR.model.PrReviewMetadata(
                prNumber,
                payload.pull_request().head().sha(),
                payload.pull_request().head().ref(),
                payload.pull_request().user().login(),
                payload.pull_request().diff_url()
        );

        PrReviewResult result = coordinatorService.review(diff, prDescription, owner, repo, metadata);

        String comment = formatComment(result);
        gitHubApiClient.postComment(owner, repo, prNumber, comment);

        return "Review posted successfully";
    }

    private void handlePushEvent(String rawBody) throws Exception {
        var payload = objectMapper.readValue(rawBody, java.util.Map.class);
        var repository = (java.util.Map<String, Object>) payload.get("repository");
        String repo = (String) repository.get("name");
        var owner = (String) ((java.util.Map<String, Object>) repository.get("owner")).get("name");

        var commits = (java.util.List<java.util.Map<String, Object>>) payload.get("commits");
        if (commits == null) return;

        for (var commit : commits) {
            List<String> added = (List<String>) commit.get("added");
            List<String> modified = (List<String>) commit.get("modified");
            List<String> removed = (List<String>) commit.get("removed");

            if (added != null) added.forEach(f -> repoIndexingService.indexFile(owner, repo, f));
            if (modified != null) modified.forEach(f -> repoIndexingService.indexFile(owner, repo, f));
            if (removed != null) removed.forEach(f -> repoIndexingService.removeFile(owner, repo, f));
        }
    }

    private boolean isSignatureValid(String payload, String signatureHeader) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        String computedSignature = "sha256=" + bytesToHex(hash);
        return computedSignature.equals(signatureHeader);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    private String formatComment(PrReviewResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 🤖 Automated PR Review\n\n");
        sb.append("**Verdict:** ").append(result.verdict()).append("\n\n");
        sb.append(result.summary()).append("\n\n");
        result.findings().forEach(f ->
                sb.append("- **[").append(f.severity()).append("]** `")
                        .append(f.file()).append("` (line ").append(f.line()).append("): ")
                        .append(f.message()).append("\n")
        );
        return sb.toString();
    }
}