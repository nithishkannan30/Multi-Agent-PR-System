package com.example.MultiAgentsForPR.rag;

import com.example.MultiAgentsForPR.github.GitHubApiClient;
import com.example.MultiAgentsForPR.persistence.IndexedFileEntity;
import com.example.MultiAgentsForPR.persistence.IndexedFileRepository;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class RepoIndexingService {

    private final GitHubApiClient gitHubApiClient;
    private final VectorStore vectorStore;
    private final IndexedFileRepository indexedFileRepository;

    public RepoIndexingService(GitHubApiClient gitHubApiClient, VectorStore vectorStore,
                               IndexedFileRepository indexedFileRepository) {
        this.gitHubApiClient = gitHubApiClient;
        this.vectorStore = vectorStore;
        this.indexedFileRepository = indexedFileRepository;
    }

    public void indexRepoIfNeeded(String owner, String repo) {
        List<IndexedFileEntity> existing = indexedFileRepository.findByOwnerAndRepo(owner, repo);
        if (!existing.isEmpty()) {
            return; // already indexed at least once
        }
        System.out.println("Indexing repo for the first time: " + owner + "/" + repo);
        List<String> files = gitHubApiClient.listRepoFiles(owner, repo);
        for (String filePath : files) {
            indexFile(owner, repo, filePath);
        }
        System.out.println("Finished indexing " + files.size() + " files for " + owner + "/" + repo);
    }

    public void indexFile(String owner, String repo, String filePath) {
        String content = gitHubApiClient.getFileContent(owner, repo, filePath);
        if (content == null || content.startsWith("[Could not fetch")) return;

        deleteExistingChunks(owner, repo, filePath);

        List<String> chunks = CodeChunker.chunk(content);
        List<Document> documents = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            String id = buildChunkId(owner, repo, filePath, i);
            Document doc = new Document(id, chunks.get(i), java.util.Map.of(
                    "owner", owner, "repo", repo, "filePath", filePath, "repoKey", owner + "/" + repo
            ));
            documents.add(doc);
        }
        if (!documents.isEmpty()) {
            vectorStore.add(documents);
        }

        Optional<IndexedFileEntity> existingEntry =
                indexedFileRepository.findByOwnerAndRepoAndFilePath(owner, repo, filePath);
        if (existingEntry.isPresent()) {
            existingEntry.get().setChunkCount(chunks.size());
            indexedFileRepository.save(existingEntry.get());
        } else {
            indexedFileRepository.save(new IndexedFileEntity(owner, repo, filePath, chunks.size()));
        }
    }

    public void removeFile(String owner, String repo, String filePath) {
        deleteExistingChunks(owner, repo, filePath);
        indexedFileRepository.findByOwnerAndRepoAndFilePath(owner, repo, filePath)
                .ifPresent(indexedFileRepository::delete);
    }

    private void deleteExistingChunks(String owner, String repo, String filePath) {
        Optional<IndexedFileEntity> existing =
                indexedFileRepository.findByOwnerAndRepoAndFilePath(owner, repo, filePath);
        if (existing.isPresent()) {
            List<String> idsToDelete = new ArrayList<>();
            for (int i = 0; i < existing.get().getChunkCount(); i++) {
                idsToDelete.add(buildChunkId(owner, repo, filePath, i));
            }
            vectorStore.delete(idsToDelete);
        }
    }

    private String buildChunkId(String owner, String repo, String filePath, int index) {
        return (owner + "_" + repo + "_" + filePath + "_" + index)
                .replaceAll("[^a-zA-Z0-9_]", "_");
    }
}