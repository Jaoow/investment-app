package dev.jaoow.investmentapp.application.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record RebalanceSaleRequest(
        @NotNull Mode mode,
        @DecimalMin("0.01") BigDecimal withdrawalAmount
) {
    public enum Mode {
        REALLOCATION,
        PROFIT_TAKING
    }
}
