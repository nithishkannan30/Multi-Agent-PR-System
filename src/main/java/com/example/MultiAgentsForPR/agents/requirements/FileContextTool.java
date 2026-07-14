package com.example.MultiAgentsForPR.agents.requirements;

import com.example.MultiAgentsForPR.github.GitHubApiClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;

import java.util.List;
import java.util.stream.Collectors;

public class FileContextTool {

    private final GitHubApiClient gitHubApiClient;
    private final VectorStore vectorStore;
    private final String owner;
    private final String repo;

    public FileContextTool(GitHubApiClient gitHubApiClient, VectorStore vectorStore, String owner, String repo) {
        this.gitHubApiClient = gitHubApiClient;
        this.vectorStore = vectorStore;
        this.owner = owner;
        this.repo = repo;
    }

    @Tool(description = "Fetch the full current content of a file by its path, for context beyond the diff")
    public String getFullFile(String filePath) {
        if (owner == null || repo == null) return "[No repository context available]";
        return ContextTruncator.truncate(gitHubApiClient.getFileContent(owner, repo, filePath));
    }

    @Tool(description = "Semantically search the repository for code related to a concept, method, or behavior — even if you don't know the exact file or class name")
    public String searchRepository(String query) {
        if (owner == null || repo == null) return "[No repository context available]";

        FilterExpressionBuilder b = new FilterExpressionBuilder();
        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(5)
                .filterExpression(b.eq("repoKey", owner + "/" + repo).build())
                .build();

        List<org.springframework.ai.document.Document> results = vectorStore.similaritySearch(request);

        if (results.isEmpty()) return "[No related code found for: " + query + "]";

        return results.stream()
                .map(doc -> "File: " + doc.getMetadata().get("filePath") + "\n" + doc.getText())
                .collect(Collectors.joining("\n---\n"));
    }

    @Tool(description = "Fetch the repository's README for overall project context")
    public String getReadme() {
        if (owner == null || repo == null) return "[No repository context available]";
        return ContextTruncator.truncate(gitHubApiClient.getReadme(owner, repo));
    }
}