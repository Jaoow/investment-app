package dev.jaoow.investmentapp.application.dto.response.summary;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AssetSummaryResponse {
    private String tickerSymbol;
    private BigDecimal totalInvested;
    private BigDecimal currentValue;
    private BigDecimal profitOrLoss;
    private BigDecimal percentageChange;

    // Fields from Brapi API
    private String shortName;
    private String longName;
    private BigDecimal regularMarketChange;
    private BigDecimal regularMarketChangePercent;
    private BigDecimal regularMarketDayHigh;
    private BigDecimal regularMarketDayLow;
    private BigDecimal regularMarketVolume;
    private BigDecimal regularMarketOpen;
    private String logourl;

    // Fields from Ticker
    private String category;
    private String sector;
    private String subSector;
    private BigDecimal priceCeiling;
}
