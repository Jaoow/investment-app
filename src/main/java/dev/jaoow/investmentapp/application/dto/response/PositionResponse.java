package dev.jaoow.investmentapp.application.dto.response;

import java.math.BigDecimal;

public record PositionResponse(String tickerSymbol, BigDecimal quantity, BigDecimal totalCost, long revision) {
}
