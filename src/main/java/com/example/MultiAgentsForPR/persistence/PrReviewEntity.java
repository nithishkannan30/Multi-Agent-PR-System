package com.example.MultiAgentsForPR.persistence;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pr_reviews")
public class PrReviewEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String owner;
    private String repo;
    private Integer prNumber;
    private String commitSha;
    private String branch;
    private String author;

    @Column(columnDefinition = "TEXT")
    private String prDescription;

    // Lightweight reference instead of storing the full diff text
    @Column(columnDefinition = "TEXT")
    private String diffUrl;

    private String verdict;

    @Column(columnDefinition = "TEXT")
    private String summary;

    private Long durationMs;

    private LocalDateTime createdAt;

    // EAGER here is a deliberate tradeoff: review findings are small in number (typically <20),
    // so eager loading avoids LazyInitializationException when serializing to JSON in a REST response,
    // at the cost of always fetching findings even when not needed. For a larger findings set,
    // this should switch to LAZY + a DTO projection instead.
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "review_id")
    private List<ReviewFindingEntity> findings = new ArrayList<>();

    public PrReviewEntity() {}

    public Long getId() { return id; }
    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }
    public String getRepo() { return repo; }
    public void setRepo(String repo) { this.repo = repo; }
    public Integer getPrNumber() { return prNumber; }
    public void setPrNumber(Integer prNumber) { this.prNumber = prNumber; }
    public String getCommitSha() { return commitSha; }
    public void setCommitSha(String commitSha) { this.commitSha = commitSha; }
    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public String getPrDescription() { return prDescription; }
    public void setPrDescription(String prDescription) { this.prDescription = prDescription; }
    public String getDiffUrl() { return diffUrl; }
    public void setDiffUrl(String diffUrl) { this.diffUrl = diffUrl; }
    public String getVerdict() { return verdict; }
    public void setVerdict(String verdict) { this.verdict = verdict; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public List<ReviewFindingEntity> getFindings() { return findings; }
    public void setFindings(List<ReviewFindingEntity> findings) { this.findings = findings; }
}