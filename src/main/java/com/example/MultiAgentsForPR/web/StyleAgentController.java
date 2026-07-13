package com.example.MultiAgentsForPR.web;

import com.example.MultiAgentsForPR.agents.style.StyleAgentService;
import com.example.MultiAgentsForPR.model.DiffRequest;
import com.example.MultiAgentsForPR.model.ReviewFinding;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class StyleAgentController {

    private final StyleAgentService styleAgentService;

    public StyleAgentController(StyleAgentService styleAgentService) {
        this.styleAgentService = styleAgentService;
    }

    @PostMapping("/agents/style/review")
    public List<ReviewFinding> review(@RequestBody DiffRequest request) {
        return styleAgentService.reviewDiff(request.diff());
    }
}
