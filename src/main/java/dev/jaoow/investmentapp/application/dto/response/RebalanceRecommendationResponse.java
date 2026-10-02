package dev.jaoow.investmentapp.application.dto.response;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RebalanceRecommendationResponse {
    private String tickerSymbol;
    private String action; // "BUY" or "SELL"
    private BigDecimal quantity;
    private BigDecimal currentPercentage;
    private BigDecimal targetPercentage;
}
