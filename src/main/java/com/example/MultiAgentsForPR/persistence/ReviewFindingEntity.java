package com.example.MultiAgentsForPR.persistence;

import com.example.MultiAgentsForPR.model.Severity;
import jakarta.persistence.*;

@Entity
@Table(name = "review_findings")
public class ReviewFindingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String agentName;

    @Enumerated(EnumType.STRING)
    private Severity severity;

    private String file;

    private Integer line;

    @Column(columnDefinition = "TEXT")
    private String message;

    public ReviewFindingEntity() {}

    public ReviewFindingEntity(String agentName, Severity severity, String file, Integer line, String message) {
        this.agentName = agentName;
        this.severity = severity;
        this.file = file;
        this.line = line;
        this.message = message;
    }

    public Long getId() { return id; }
    public String getAgentName() { return agentName; }
    public Severity getSeverity() { return severity; }
    public String getFile() { return file; }
    public Integer getLine() { return line; }
    public String getMessage() { return message; }
}