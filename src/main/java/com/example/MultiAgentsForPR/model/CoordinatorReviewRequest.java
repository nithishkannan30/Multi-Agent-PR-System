package com.example.MultiAgentsForPR.model;

public record CoordinatorReviewRequest(String diff, String prDescription, String owner, String repo) {}