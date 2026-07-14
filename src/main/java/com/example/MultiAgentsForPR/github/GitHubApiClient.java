package com.example.MultiAgentsForPR.github;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class GitHubApiClient {

    private final RestClient restClient;

    public GitHubApiClient(@Value("${github.token}") String githubToken) {
        this.restClient = RestClient.builder()
                .baseUrl("https://api.github.com")
                .defaultHeader("Authorization", "Bearer " + githubToken)
                .defaultHeader("Accept", "application/vnd.github+json")
                .build();
    }

    public String getPrDiff(String owner, String repo, int prNumber) {
        return restClient.get()
                .uri("/repos/{owner}/{repo}/pulls/{prNumber}", owner, repo, prNumber)
                .header("Accept", "application/vnd.github.diff")
                .retrieve()
                .body(String.class);
    }

    public void postComment(String owner, String repo, int prNumber, String commentBody) {
        restClient.post()
                .uri("/repos/{owner}/{repo}/issues/{prNumber}/comments", owner, repo, prNumber)
                .body(java.util.Map.of("body", commentBody))
                .retrieve()
                .toBodilessEntity();
    }

    public String getFileContent(String owner, String repo, String path) {
        try {
            return restClient.get()
                    .uri("/repos/{owner}/{repo}/contents/{path}", owner, repo, path)
                    .header("Accept", "application/vnd.github.raw")
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            return "[Could not fetch file: " + path + " - " + e.getMessage() + "]";
        }
    }

    public String getReadme(String owner, String repo) {
        try {
            return restClient.get()
                    .uri("/repos/{owner}/{repo}/readme", owner, repo)
                    .header("Accept", "application/vnd.github.raw")
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            return "[No README found or accessible]";
        }
    }

    public String searchCodeInRepo(String owner, String repo, String query) {
        try {
            String searchQuery = query + " repo:" + owner + "/" + repo;
            var response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search/code")
                            .queryParam("q", searchQuery)
                            .build())
                    .header("Accept", "application/vnd.github+json")
                    .retrieve()
                    .body(String.class);
            return response;
        } catch (Exception e) {
            return "[Search failed: " + e.getMessage() + "]";
        }
    }

    public String getDefaultBranch(String owner, String repo) {
        var response = restClient.get()
                .uri("/repos/{owner}/{repo}", owner, repo)
                .retrieve()
                .body(java.util.Map.class);
        return (String) response.get("default_branch");
    }

    public java.util.List<String> listRepoFiles(String owner, String repo) {
        String branch = getDefaultBranch(owner, repo);
        var response = restClient.get()
                .uri("/repos/{owner}/{repo}/git/trees/{branch}?recursive=1", owner, repo, branch)
                .retrieve()
                .body(java.util.Map.class);

        java.util.List<java.util.Map<String, Object>> tree = (java.util.List<java.util.Map<String, Object>>) response.get("tree");
        java.util.List<String> filePaths = new java.util.ArrayList<>();

        for (var item : tree) {
            String type = (String) item.get("type");
            String path = (String) item.get("path");
            if ("blob".equals(type) && isCodeFile(path)) {
                filePaths.add(path);
            }
        }
        return filePaths;
    }

    private boolean isCodeFile(String path) {
        return path.endsWith(".java") || path.endsWith(".properties")
                || path.endsWith(".yml") || path.endsWith(".xml") || path.endsWith(".md");
    }
}