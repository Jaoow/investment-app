package dev.jaoow.investmentapp.application.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record RecommendationCalculationResponse(
        BigDecimal investmentAmount,
        BigDecimal allocatedAmount,
        BigDecimal remainingAmount,
        List<Recommendation> recommendations,
        List<ExcludedSuggestionResponse> excludedAssets,
        String disclaimer
) {
    public record Recommendation(
            String ticker,
            String assetName,
            BigDecimal score,
            String recommendationLevel,
            BigDecimal suggestedAmount,
            BigDecimal suggestedQuantity,
            BigDecimal currentAllocation,
            BigDecimal targetAllocation,
            BigDecimal currentPrice,
            BigDecimal ceilingPrice,
            BigDecimal dailyVariation,
            List<String> explanation,
            String quoteProvider,
            Instant quoteObservedAt
    ) {
    }
}