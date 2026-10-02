package dev.jaoow.investmentapp.application.service.recommendation;

import java.math.BigDecimal;

public record RecommendationAllocation(
        PurchaseCandidate candidate,
        BigDecimal quantity,
        BigDecimal estimatedValue
) {
}
