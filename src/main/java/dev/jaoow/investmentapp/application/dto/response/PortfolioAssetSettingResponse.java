package dev.jaoow.investmentapp.application.dto.response;

import dev.jaoow.investmentapp.domain.model.AssetCategory;
import java.math.BigDecimal;

public record PortfolioAssetSettingResponse(
        String tickerSymbol, AssetCategory category,
        BigDecimal categoryTargetPercentage, BigDecimal assetTargetPercentage,
        BigDecimal allocationPercentage, BigDecimal priceCeiling) {
}
