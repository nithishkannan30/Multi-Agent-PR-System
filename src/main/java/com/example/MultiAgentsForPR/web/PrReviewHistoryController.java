package com.example.MultiAgentsForPR.web;

import com.example.MultiAgentsForPR.persistence.PrReviewEntity;
import com.example.MultiAgentsForPR.persistence.PrReviewRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class PrReviewHistoryController {

    private final PrReviewRepository prReviewRepository;

    public PrReviewHistoryController(PrReviewRepository prReviewRepository) {
        this.prReviewRepository = prReviewRepository;
    }

    @GetMapping("/reviews/history")
    public List<PrReviewEntity> getHistory() {
        return prReviewRepository.findAll();
    }
}