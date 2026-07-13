package com.example.MultiAgentsForPR.model;

import java.util.List;

public record PrReviewResult(
        Verdict verdict,
        List<ReviewFinding> findings,
        String summary
) {}