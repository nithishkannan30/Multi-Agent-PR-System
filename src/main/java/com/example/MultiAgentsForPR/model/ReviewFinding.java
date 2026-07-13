package com.example.MultiAgentsForPR.model;


public record ReviewFinding(
        String agentName,
        Severity severity,
        String file,
        Integer line,
        String message
) {}