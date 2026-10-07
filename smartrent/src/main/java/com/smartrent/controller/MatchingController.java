package com.smartrent.controller;

import com.smartrent.dto.MatchRequest;
import com.smartrent.dto.MatchResponse;
import com.smartrent.service.MatchingService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/matches")
public class MatchingController {

    private final MatchingService matchingService;

    public MatchingController(MatchingService matchingService) {
        this.matchingService = matchingService;
    }

    @PostMapping
    public List<MatchResponse> findMatches(
            @Valid @RequestBody MatchRequest request) {

        return matchingService.findMatches(request);
    }
}