package com.example.MultiAgentsForPR.web;

import com.example.MultiAgentsForPR.agents.requirements.RequirementsAgentService;
import com.example.MultiAgentsForPR.model.RequirementsReviewRequest;
import com.example.MultiAgentsForPR.model.ReviewFinding;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class RequirementsAgentController {

    private final RequirementsAgentService requirementsAgentService;

    public RequirementsAgentController(RequirementsAgentService requirementsAgentService) {
        this.requirementsAgentService = requirementsAgentService;
    }

    @PostMapping("/agents/requirements/review")
    public List<ReviewFinding> review(@RequestBody RequirementsReviewRequest request) {
        return requirementsAgentService.review(
                request.diff(),
                request.prDescription(),
                request.owner(),
                request.repo()
        );
    }
}