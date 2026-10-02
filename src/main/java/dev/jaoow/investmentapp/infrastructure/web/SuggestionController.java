package dev.jaoow.investmentapp.infrastructure.web;

import dev.jaoow.investmentapp.application.dto.request.SuggestionRequest;
import dev.jaoow.investmentapp.application.dto.response.SuggestionResponse;
import dev.jaoow.investmentapp.application.service.recommendation.SuggestionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/portfolio/{portfolioId}/suggestions")
public class SuggestionController {

    private final SuggestionService suggestionService;

    public SuggestionController(SuggestionService suggestionService) {
        this.suggestionService = suggestionService;
    }

    @PostMapping
    public SuggestionResponse suggest(
            @PathVariable Long portfolioId,
            @Valid @RequestBody SuggestionRequest request) {
        return suggestionService.suggest(portfolioId, request);
    }
}
