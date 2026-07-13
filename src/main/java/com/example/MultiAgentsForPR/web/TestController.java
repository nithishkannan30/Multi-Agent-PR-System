package com.example.MultiAgentsForPR.web;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    private final ChatClient chatClient;

    public TestController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @GetMapping("/test-ai")
    public String testAi() {
        return chatClient.prompt()
                .user("Say hello in one sentence.")
                .call()
                .content();
    }
}
