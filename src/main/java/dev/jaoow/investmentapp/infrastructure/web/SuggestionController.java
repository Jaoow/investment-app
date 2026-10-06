package dev.jaoow.investmentapp.infrastructure.web;

import dev.jaoow.investmentapp.application.dto.request.SuggestionRequest;
import dev.jaoow.investmentapp.application.dto.request.RecommendationCalculationRequest;
import dev.jaoow.investmentapp.application.dto.response.RecommendationCalculationResponse;
import dev.jaoow.investmentapp.application.dto.response.SuggestionResponse;
import dev.jaoow.investmentapp.application.service.recommendation.SuggestionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

@RestController
@RequestMapping("/v1/portfolio/{portfolioId}")
public class SuggestionController {

    private final SuggestionService suggestionService;

    public SuggestionController(SuggestionService suggestionService) {
        this.suggestionService = suggestionService;
    }

    @PostMapping("/suggestions")
    public SuggestionResponse suggest(
            @PathVariable Long portfolioId,
            @Valid @RequestBody SuggestionRequest request) {
        return suggestionService.suggest(portfolioId, request);
    }

    @PostMapping("/recommendations/calculate")
    public RecommendationCalculationResponse calculateRecommendations(
            @PathVariable Long portfolioId,
            @Valid @RequestBody RecommendationCalculationRequest request) {
        SuggestionRequest suggestionRequest = new SuggestionRequest();
        suggestionRequest.setAmount(request.investmentAmount());
        suggestionRequest.setCurrency("BRL");
        SuggestionResponse result = suggestionService.suggest(portfolioId, suggestionRequest);

        var recommendations = result.getItems().stream().map(item -> {
            BigDecimal score = item.getScore().movePointRight(2).setScale(2, RoundingMode.HALF_UP);
            return new RecommendationCalculationResponse.Recommendation(
                    item.getTickerSymbol(),
                    item.getAssetName(),
                    score,
                    recommendationLevel(score),
                    item.getEstimatedValue(),
                    item.getQuantity(),
                    item.getCurrentPercentage(),
                    item.getTargetPercentage(),
                    item.getUnitPrice(),
                    item.getCeilingPrice(),
                    item.getDailyChangePercent(),
                    item.getReasons(),
                    item.getQuoteProvider(),
                    item.getQuoteObservedAt());
        }).toList();

        return new RecommendationCalculationResponse(
                result.getRequestedAmount(),
                result.getAllocatedAmount(),
                result.getRemainingAmount(),
                recommendations,
                result.getExcludedAssets(),
                result.getDisclaimer());
    }

    private String recommendationLevel(BigDecimal score) {
        if (score.compareTo(BigDecimal.valueOf(80)) >= 0) return "STRONG_BUY";
        if (score.compareTo(BigDecimal.valueOf(60)) >= 0) return "MODERATE_BUY";
        if (score.compareTo(BigDecimal.valueOf(40)) >= 0) return "NEUTRAL";
        return "LOW_PRIORITY";
    }
}
