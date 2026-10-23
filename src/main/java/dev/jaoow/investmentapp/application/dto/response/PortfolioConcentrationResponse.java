package dev.jaoow.investmentapp.application.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record PortfolioConcentrationResponse(
        BigDecimal totalValue,
        int assetsCount,
        BigDecimal effectiveAssets,
        BigDecimal topAssetPercentage,
        BigDecimal topThreeAssetsPercentage,
        Level level,
        List<Slice> assets,
        List<Slice> sectors,
        List<Slice> classes,
        List<Alert> alerts,
        List<Limit> limits
) {
    public enum Level {
        OK,
        WARNING,
        CRITICAL
    }

    public enum Dimension {
        ASSET,
        SECTOR,
        CLASS
    }

    public record Slice(
            String key,
            String label,
            BigDecimal value,
            BigDecimal percentage,
            BigDecimal targetPercentage,
            int assetsCount,
            List<String> tickers,
            Level level
    ) {
    }

    public record Alert(
            Dimension dimension,
            String key,
            String label,
            BigDecimal percentage,
            Level level,
            String message
    ) {
    }

    public record Limit(
            Dimension dimension,
            BigDecimal warningPercentage,
            BigDecimal criticalPercentage
    ) {
    }
}
