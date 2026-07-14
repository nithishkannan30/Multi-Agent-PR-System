package com.example.MultiAgentsForPR.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WebhookPayload(
        String action,
        PullRequest pull_request,
        Repository repository
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PullRequest(
            int number,
            String title,
            String body
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Repository(
            String name,
            Owner owner
    ) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Owner(String login) {}
    }
}