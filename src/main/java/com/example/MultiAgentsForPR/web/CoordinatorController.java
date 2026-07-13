package com.example.MultiAgentsForPR.web;

import com.example.MultiAgentsForPR.coordinator.CoordinatorService;
import com.example.MultiAgentsForPR.model.CoordinatorReviewRequest;
import com.example.MultiAgentsForPR.model.PrReviewResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CoordinatorController {

    private final CoordinatorService coordinatorService;

    public CoordinatorController(CoordinatorService coordinatorService) {
        this.coordinatorService = coordinatorService;
    }

    @PostMapping("/agents/coordinate/review")
    public PrReviewResult review(@RequestBody CoordinatorReviewRequest request) {
        return coordinatorService.review(request.diff(), request.prDescription());
    }
}