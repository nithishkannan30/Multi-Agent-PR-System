package com.example.MultiAgentsForPR.agents.requirements;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class FileContextTool {

    @Tool(description = "Fetch the full content of a file by filename, to get context beyond just the diff")
    public String getFileContent(String fileName) {
        // STUB for now — Module 1 will replace this with a real GitHub API call.
        return "// [MOCK] Full content of " + fileName + " would appear here once GitHub integration is wired in Module 1.";
    }
}