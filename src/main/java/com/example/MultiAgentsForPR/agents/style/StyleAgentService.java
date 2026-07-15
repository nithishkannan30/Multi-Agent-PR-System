package com.example.MultiAgentsForPR.agents.style;
import com.example.MultiAgentsForPR.metrics.ReviewMetrics;
import com.example.MultiAgentsForPR.model.ReviewFinding;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

@Service
public class StyleAgentService {

    private final ChatClient chatClient;
    private final String systemPrompt;
    private final ReviewMetrics metrics;
    public StyleAgentService(ChatClient.Builder builder,
                             @Value("classpath:prompts/style-agent-system-prompt") Resource promptResource,
                             ReviewMetrics metrics) throws IOException {
        this.chatClient = builder.build();
        this.systemPrompt = promptResource.getContentAsString(StandardCharsets.UTF_8);
        this.metrics=metrics;
    }


    @CircuitBreaker(name = "groqLLM", fallbackMethod = "circuitBreakerFallback")
    @Retryable(
            retryFor = Exception.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public List<ReviewFinding> reviewDiff(String diff) {
        return chatClient.prompt()
                .system(systemPrompt)
                .user(u -> u.text("{diff}").param("diff", diff))
                .call()
                .entity(new ParameterizedTypeReference<List<ReviewFinding>>() {});
    }


    @Recover
    public List<ReviewFinding> recover(Exception e, String diff) {
        System.err.println("StyleAgent failed after retries: " + e.getMessage());
        return Collections.emptyList();
    }

    public List<ReviewFinding> circuitBreakerFallback(String diff, Throwable t) {
        System.err.println("StyleAgent circuit breaker OPEN - skipping call: " + t.getMessage());
        metrics.incrementCircuitBreakerFallback("StyleAgent");
        return Collections.emptyList();
    }
}