package dev.jaoow.investmentapp.application.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
public class SuggestionItemResponse {
    private String tickerSymbol;
    private String assetName;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal estimatedValue;
    private BigDecimal score;
    private BigDecimal ceilingPrice;
    private BigDecimal ceilingDistancePercentage;
    private BigDecimal dailyChangePercent;
    private BigDecimal currentPercentage;
    private BigDecimal targetPercentage;
    private List<String> reasons;
    private String quoteProvider;
    private Instant quoteObservedAt;
}
