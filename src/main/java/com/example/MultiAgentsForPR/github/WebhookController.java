package com.example.MultiAgentsForPR.github;

import com.example.MultiAgentsForPR.coordinator.CoordinatorService;
import com.example.MultiAgentsForPR.model.PrReviewResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@RestController
public class WebhookController {

    private final CoordinatorService coordinatorService;
    private final GitHubApiClient gitHubApiClient;
    private final ObjectMapper objectMapper;
    private final String webhookSecret;

    public WebhookController(CoordinatorService coordinatorService,
                             GitHubApiClient gitHubApiClient,
                             ObjectMapper objectMapper,
                             @Value("${github.webhook.secret}") String webhookSecret) {
        this.coordinatorService = coordinatorService;
        this.gitHubApiClient = gitHubApiClient;
        this.objectMapper = objectMapper;
        this.webhookSecret = webhookSecret;
    }

    @PostMapping("/webhooks/github")
    public String handleWebhook(@RequestBody String rawBody,
                                @RequestHeader("X-Hub-Signature-256") String signature,
                                @RequestHeader("X-GitHub-Event") String eventType) throws Exception {

        if (!isSignatureValid(rawBody, signature)) {
            return "Invalid signature";
        }

        if (!"pull_request".equals(eventType)) {
            return "Ignored - not a pull_request event";
        }

        WebhookPayload payload = objectMapper.readValue(rawBody, WebhookPayload.class);

        if (!"opened".equals(payload.action()) && !"synchronize".equals(payload.action())) {
            return "Ignored - action was " + payload.action();
        }

        String owner = payload.repository().owner().login();
        String repo = payload.repository().name();
        int prNumber = payload.pull_request().number();
        String prDescription = payload.pull_request().body() != null ? payload.pull_request().body() : "";

        String diff = gitHubApiClient.getPrDiff(owner, repo, prNumber);

        PrReviewResult result = coordinatorService.review(diff, prDescription);

        String comment = formatComment(result);
        gitHubApiClient.postComment(owner, repo, prNumber, comment);

        return "Review posted successfully";
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