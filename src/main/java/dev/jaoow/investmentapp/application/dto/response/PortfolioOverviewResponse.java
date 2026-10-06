package dev.jaoow.investmentapp.application.dto.response;

import dev.jaoow.investmentapp.application.dto.response.summary.AssetSummaryResponse;

import java.math.BigDecimal;
import java.util.List;

public record PortfolioOverviewResponse(
        Long portfolioId,
        BigDecimal patrimony,
        BigDecimal investedAmount,
        BigDecimal profitAmount,
        BigDecimal profitPercentage,
        int assetsCount,
        List<AssetSummaryResponse> assets,
        List<PortfolioClassResponse> classes
) {
}