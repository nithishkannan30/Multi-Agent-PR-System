package com.example.MultiAgentsForPR.agents.requirements;

import com.example.MultiAgentsForPR.model.ReviewFinding;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class RequirementsAgentService {

    private final ChatClient chatClient;
    private final String systemPrompt;
    private final FileContextTool fileContextTool;

    public RequirementsAgentService(ChatClient.Builder builder,
                                    @Value("classpath:prompts/requirements-agent-system-prompt.txt") Resource promptResource,
                                    FileContextTool fileContextTool) throws IOException {
        this.chatClient = builder.build();
        this.systemPrompt = promptResource.getContentAsString(StandardCharsets.UTF_8);
        this.fileContextTool = fileContextTool;
    }

    @Retryable(
            retryFor = Exception.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public List<ReviewFinding> review(String diff, String prDescription) {
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
}