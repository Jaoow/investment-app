package dev.jaoow.investmentapp.application.dto.response;

import java.math.BigDecimal;

public record PortfolioClassResponse(
        String id,
        String name,
        BigDecimal currentPercentage,
        BigDecimal targetPercentage,
        BigDecimal currentValue,
        BigDecimal profitability,
        int assetsCount,
        BigDecimal investedAmount,
        BigDecimal profitAmount
) {
}