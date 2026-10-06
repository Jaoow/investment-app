package dev.jaoow.investmentapp.application.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record RecommendationCalculationRequest(
        @NotNull @DecimalMin("0.01") BigDecimal investmentAmount
) {
}