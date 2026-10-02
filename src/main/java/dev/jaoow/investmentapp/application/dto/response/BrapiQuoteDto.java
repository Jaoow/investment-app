package dev.jaoow.investmentapp.application.dto.response;

import lombok.Data;

import java.time.Instant;
import java.math.BigDecimal;
import java.util.List;

@Data
public class BrapiQuoteDto {
    private String symbol;
    private String currency;
    private String regularMarketTime;
    private Instant fetchedAt;
    private String shortName;
    private String longName;
    private BigDecimal regularMarketPrice;
    private BigDecimal regularMarketChange;
    private BigDecimal regularMarketChangePercent;
    private BigDecimal regularMarketDayHigh;
    private BigDecimal regularMarketDayLow;
    private String regularMarketDayRange;
    private BigDecimal regularMarketVolume;
    private BigDecimal regularMarketPreviousClose;
    private BigDecimal regularMarketOpen;
    private BigDecimal fiftyTwoWeekLow;
    private BigDecimal fiftyTwoWeekHigh;
    private List<HistoricalDataPrice> historicalDataPrice;
    private String logourl;

    @Data
    public static class HistoricalDataPrice {
        private Long date;
        private BigDecimal open;
        private BigDecimal high;
        private BigDecimal low;
        private BigDecimal close;
        private BigDecimal volume;
        private BigDecimal adjustedClose;
    }
}
