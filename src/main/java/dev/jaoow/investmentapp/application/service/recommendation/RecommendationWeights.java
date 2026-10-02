package dev.jaoow.investmentapp.application.service.recommendation;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
@Component
public class RecommendationWeights {
    private final BigDecimal ceilingDistance;
    private final BigDecimal dailyDrop;
    private final BigDecimal allocationGap;

    public RecommendationWeights(
            @Value("${investize.recommendations.weights.ceiling-distance:0.50}") BigDecimal ceilingDistance,
            @Value("${investize.recommendations.weights.daily-drop:0.20}") BigDecimal dailyDrop,
            @Value("${investize.recommendations.weights.allocation-gap:0.30}") BigDecimal allocationGap) {
        this.ceilingDistance = requireNonNegative("ceiling-distance", ceilingDistance);
        this.dailyDrop = requireNonNegative("daily-drop", dailyDrop);
        this.allocationGap = requireNonNegative("allocation-gap", allocationGap);
        if (ceilingDistance.add(dailyDrop).add(allocationGap).compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("At least one recommendation weight must be greater than zero.");
        }
    }

    public BigDecimal score(BigDecimal ceilingSignal, BigDecimal dailyDropSignal, BigDecimal allocationGapSignal) {
        BigDecimal weightedSum = ceilingDistance.multiply(ceilingSignal)
                .add(allocationGap.multiply(allocationGapSignal));
        BigDecimal availableWeight = ceilingDistance.add(allocationGap);

        if (dailyDropSignal != null) {
            weightedSum = weightedSum.add(dailyDrop.multiply(dailyDropSignal));
            availableWeight = availableWeight.add(dailyDrop);
        }

        return availableWeight.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : weightedSum.divide(availableWeight, 8, java.math.RoundingMode.HALF_UP);
    }

    private BigDecimal requireNonNegative(String name, BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Recommendation weight " + name + " must be non-negative.");
        }
        return value;
    }
}
