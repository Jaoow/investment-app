package dev.jaoow.investmentapp.application.model;

import java.math.BigDecimal;
import java.time.Instant;

public record MarketQuote(
        String tickerSymbol,
        BigDecimal price,
        String currency,
        BigDecimal dailyChangePercent,
        Instant observedAt,
        Instant fetchedAt,
        String provider
) {
}
