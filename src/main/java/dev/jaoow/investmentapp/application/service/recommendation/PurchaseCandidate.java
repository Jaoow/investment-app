package dev.jaoow.investmentapp.application.service.recommendation;

import java.math.BigDecimal;
import java.util.List;

public record PurchaseCandidate(
        String tickerSymbol,
        BigDecimal price,
        BigDecimal ceiling,
        BigDecimal score,
        BigDecimal currentPercentage,
        BigDecimal targetPercentage,
        BigDecimal ceilingDistancePercentage,
        BigDecimal dailyChangePercent,
        BigDecimal targetGapAmount,
        List<String> reasons
) {
}
