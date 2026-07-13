package com.example.MultiAgentsForPR.agents.security;

import com.example.MultiAgentsForPR.model.ReviewFinding;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class SecurityAgentService {

    private final ChatClient chatClient;
    private final String systemPrompt;

    public SecurityAgentService(ChatClient.Builder builder,
                                @Value("classpath:prompts/security-agent-system-prompt.txt") Resource promptResource) throws IOException {
        this.chatClient = builder.build();
        this.systemPrompt = promptResource.getContentAsString(StandardCharsets.UTF_8);
    }

    public List<ReviewFinding> reviewDiff(String diff) {
        return chatClient.prompt()
                .system(systemPrompt)
                .user(u -> u.text("{diff}").param("diff", diff))
                .call()
                .entity(new ParameterizedTypeReference<List<ReviewFinding>>() {});
    }
}