package com.example.MultiAgentsForPR.web;

import com.example.MultiAgentsForPR.coordinator.CoordinatorService;
import com.example.MultiAgentsForPR.model.CoordinatorReviewRequest;
import com.example.MultiAgentsForPR.model.PrReviewResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Coordinator", description = "Runs all three review agents and returns a merged verdict")
public class CoordinatorController {

    private final CoordinatorService coordinatorService;

    public CoordinatorController(CoordinatorService coordinatorService) {
        this.coordinatorService = coordinatorService;
    }

    @Operation(summary = "Review a diff with all agents", description = "Runs Style, Security, and Requirements agents in parallel and returns a combined verdict")
    @PostMapping("/agents/coordinate/review")
    public PrReviewResult review(@RequestBody CoordinatorReviewRequest request) {
        return coordinatorService.review(request.diff(), request.prDescription());
    }
}