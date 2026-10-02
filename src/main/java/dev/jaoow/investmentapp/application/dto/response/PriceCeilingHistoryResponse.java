package dev.jaoow.investmentapp.application.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record PriceCeilingHistoryResponse(
        Long id, String tickerSymbol, BigDecimal previousPrice,
        BigDecimal priceCeiling, String action, Instant changedAt) {
}
