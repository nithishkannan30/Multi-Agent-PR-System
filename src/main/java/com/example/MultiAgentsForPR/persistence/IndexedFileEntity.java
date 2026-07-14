package com.example.MultiAgentsForPR.persistence;

import jakarta.persistence.*;

@Entity
@Table(name = "indexed_files")
public class IndexedFileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String owner;
    private String repo;
    private String filePath;
    private int chunkCount;

    public IndexedFileEntity() {}

    public IndexedFileEntity(String owner, String repo, String filePath, int chunkCount) {
        this.owner = owner;
        this.repo = repo;
        this.filePath = filePath;
        this.chunkCount = chunkCount;
    }

    public Long getId() { return id; }
    public String getOwner() { return owner; }
    public String getRepo() { return repo; }
    public String getFilePath() { return filePath; }
    public int getChunkCount() { return chunkCount; }
    public void setChunkCount(int chunkCount) { this.chunkCount = chunkCount; }
}