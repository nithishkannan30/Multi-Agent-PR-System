package com.example.MultiAgentsForPR.agents.requirements;

import com.example.MultiAgentsForPR.github.GitHubApiClient;
import com.example.MultiAgentsForPR.model.ReviewFinding;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

@Service
public class RequirementsAgentService {

    private final ChatClient chatClient;
    private final String systemPrompt;
    private final GitHubApiClient gitHubApiClient;

    private final VectorStore vectorStore;

    public RequirementsAgentService(ChatClient.Builder builder,
                                    @Value("classpath:prompts/requirements-agent-system-prompt.txt") Resource promptResource,
                                    GitHubApiClient gitHubApiClient,
                                    VectorStore vectorStore) throws IOException {
        this.chatClient = builder.build();
        this.systemPrompt = promptResource.getContentAsString(StandardCharsets.UTF_8);
        this.gitHubApiClient = gitHubApiClient;
        this.vectorStore = vectorStore;
    }

    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public List<ReviewFinding> review(String diff, String prDescription, String owner, String repo) {
        FileContextTool fileContextTool = new FileContextTool(gitHubApiClient, vectorStore, owner, repo);

        String combinedInput = "PR Description: {prDescription}\n\nDiff:\n{diff}";

        return chatClient.prompt()
                .system(systemPrompt)
                .user(u -> u.text(combinedInput)
                        .param("prDescription", prDescription)
                        .param("diff", diff))
                .tools(fileContextTool)
                .call()
                .entity(new ParameterizedTypeReference<List<ReviewFinding>>() {});
    }

    @Recover
    public List<ReviewFinding> recover(Exception e, String diff, String prDescription, String owner, String repo) {
        System.err.println("RequirementsAgent failed after retries: " + e.getMessage());
        return Collections.emptyList();
    }
}