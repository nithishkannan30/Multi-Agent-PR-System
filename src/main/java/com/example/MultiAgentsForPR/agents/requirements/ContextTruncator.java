package com.example.MultiAgentsForPR.agents.requirements;

public class ContextTruncator {

    private static final int MAX_CHARS = 3000;

    public static String truncate(String content) {
        if (content == null) return "";
        if (content.length() <= MAX_CHARS) return content;
        return content.substring(0, MAX_CHARS) + "\n\n[... truncated, file too long to include fully ...]";
    }
}