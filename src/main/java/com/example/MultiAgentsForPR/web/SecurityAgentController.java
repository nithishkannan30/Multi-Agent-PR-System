package com.example.MultiAgentsForPR.web;

import com.example.MultiAgentsForPR.agents.security.SecurityAgentService;
import com.example.MultiAgentsForPR.model.DiffRequest;
import com.example.MultiAgentsForPR.model.ReviewFinding;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class SecurityAgentController {

    private final SecurityAgentService securityAgentService;

    public SecurityAgentController(SecurityAgentService securityAgentService) {
        this.securityAgentService = securityAgentService;
    }

    @PostMapping("/agents/security/review")
    public List<ReviewFinding> review(@RequestBody DiffRequest request) {
        return securityAgentService.reviewDiff(request.diff());
    }
}