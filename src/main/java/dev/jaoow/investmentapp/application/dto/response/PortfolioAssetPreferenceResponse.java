package dev.jaoow.investmentapp.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class PortfolioAssetPreferenceResponse {
    private String tickerSymbol;
    private BigDecimal priceCeiling;
}
