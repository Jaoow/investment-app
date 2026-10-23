package dev.jaoow.investmentapp.application.dto.response;

import dev.jaoow.investmentapp.application.dto.request.RebalanceSaleRequest;
import dev.jaoow.investmentapp.domain.model.AssetCategory;

import java.math.BigDecimal;
import java.util.List;

public record RebalanceSalePlanResponse(
        RebalanceSaleRequest.Mode mode,
        boolean configurationRequired,
        BigDecimal saleAmount,
        BigDecimal maxWithdrawalAmount,
        BigDecimal realizedProfit,
        BigDecimal availableProfit,
        int profitableAssets,
        BigDecimal preservedPrincipal,
        BigDecimal reinvestedAmount,
        List<Sale> sales,
        List<ExcludedSuggestionResponse> excludedAssets,
        List<CategoryImpact> categoryImpact,
        String disclaimer
) {
    public record Sale(
            String ticker,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal estimatedValue,
            BigDecimal realizedProfit,
            BigDecimal currentPercentage,
            BigDecimal targetPercentage,
            BigDecimal projectedPercentage,
            BigDecimal gainPercentage,
            List<String> explanation
    ) {
    }

    /** Category weight before and after the plan, including projected reinvestment. */
    public record CategoryImpact(
            AssetCategory category,
            BigDecimal currentPercentage,
            BigDecimal projectedPercentage
    ) {
    }
}
