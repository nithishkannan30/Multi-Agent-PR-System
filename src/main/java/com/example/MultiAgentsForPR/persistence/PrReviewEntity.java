package com.example.MultiAgentsForPR.persistence;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "pr_reviews")
public class PrReviewEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String diff;

    @Column(columnDefinition = "TEXT")
    private String prDescription;

    private String verdict;

    @Column(columnDefinition = "TEXT")
    private String findingsJson;

    @Column(columnDefinition = "TEXT")
    private String summary;

    private LocalDateTime createdAt;

    public PrReviewEntity() {}

    public PrReviewEntity(String diff, String prDescription, String verdict, String findingsJson, String summary) {
        this.diff = diff;
        this.prDescription = prDescription;
        this.verdict = verdict;
        this.findingsJson = findingsJson;
        this.summary = summary;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and setters
    public Long getId() { return id; }
    public String getDiff() { return diff; }
    public String getPrDescription() { return prDescription; }
    public String getVerdict() { return verdict; }
    public String getFindingsJson() { return findingsJson; }
    public String getSummary() { return summary; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setDiff(String diff) { this.diff = diff; }
    public void setPrDescription(String prDescription) { this.prDescription = prDescription; }
    public void setVerdict(String verdict) { this.verdict = verdict; }
    public void setFindingsJson(String findingsJson) { this.findingsJson = findingsJson; }
    public void setSummary(String summary) { this.summary = summary; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}