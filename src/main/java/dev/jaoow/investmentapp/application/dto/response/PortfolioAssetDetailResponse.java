package dev.jaoow.investmentapp.application.dto.response;

import java.math.BigDecimal;

public record PortfolioAssetDetailResponse(
        String ticker,
        String name,
        String sector,
        BigDecimal quantity,
        BigDecimal averagePrice,
        BigDecimal currentPrice,
        BigDecimal investedAmount,
        BigDecimal currentAmount,
        BigDecimal profitAmount,
        BigDecimal profitPercentage,
        BigDecimal allocationInClass,
        BigDecimal targetAllocationInClass,
        BigDecimal allocationInPortfolio,
        BigDecimal targetAllocationInPortfolio,
        BigDecimal dailyVariation
) {
}