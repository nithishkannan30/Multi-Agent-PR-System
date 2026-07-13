package com.example.MultiAgentsForPR.agents.style;
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
public class StyleAgentService {

    private final ChatClient chatClient;
    private final String systemPrompt;

    public StyleAgentService(ChatClient.Builder builder,
                             @Value("classpath:prompts/style-agent-system-prompt") Resource promptResource) throws IOException {
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