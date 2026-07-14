package com.example.MultiAgentsForPR.model;

public record RequirementsReviewRequest(String diff, String prDescription, String owner, String repo) {}