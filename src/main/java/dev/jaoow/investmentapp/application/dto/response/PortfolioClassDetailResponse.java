package dev.jaoow.investmentapp.application.dto.response;

import dev.jaoow.investmentapp.application.dto.response.summary.AssetSummaryResponse;

import java.util.List;

public record PortfolioClassDetailResponse(
        PortfolioClassResponse summary,
        List<AssetSummaryResponse> assets
) {
}