package dev.jaoow.investmentapp.application.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class BrapiFields {
    private String shortName;
    private String longName;
    private BigDecimal regularMarketPrice;
    private BigDecimal regularMarketChange;
    private BigDecimal regularMarketChangePercent;
    private BigDecimal regularMarketDayHigh;
    private BigDecimal regularMarketDayLow;
    private BigDecimal regularMarketVolume;
    private BigDecimal regularMarketOpen;
    private String logourl;
}
