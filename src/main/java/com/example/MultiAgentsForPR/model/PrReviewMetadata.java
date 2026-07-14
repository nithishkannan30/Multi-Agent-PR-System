package com.example.MultiAgentsForPR.model;

public record PrReviewMetadata(
        Integer prNumber,
        String commitSha,
        String branch,
        String author,
        String diffUrl
) {
    public static PrReviewMetadata empty() {
        return new PrReviewMetadata(null, null, null, null, null);
    }
}