package dev.jaoow.investmentapp.infrastructure.client;

import dev.jaoow.investmentapp.application.dto.response.BrapiQuoteDto;
import dev.jaoow.investmentapp.application.exception.MarketDataUnavailableException;
import dev.jaoow.investmentapp.application.model.MarketQuote;
import dev.jaoow.investmentapp.application.service.MarketQuoteProvider;
import dev.jaoow.investmentapp.application.service.QuoteService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

@Component
public class BrapiMarketQuoteProvider implements MarketQuoteProvider {

    private final QuoteService quoteService;

    public BrapiMarketQuoteProvider(QuoteService quoteService) {
        this.quoteService = quoteService;
    }

    @Override
    public MarketQuote getQuote(String tickerSymbol) {
        BrapiQuoteDto quote = quoteService.getQuote(tickerSymbol, null, null, null, null);
        if (quote.getRegularMarketPrice() == null || quote.getRegularMarketPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new MarketDataUnavailableException("Provider returned no valid price for " + tickerSymbol + ".");
        }
        if (quote.getCurrency() == null || quote.getCurrency().isBlank()) {
            throw new MarketDataUnavailableException("Provider returned no currency for " + tickerSymbol + ".");
        }
        if (quote.getRegularMarketTime() == null || quote.getFetchedAt() == null) {
            throw new MarketDataUnavailableException("Provider omitted quote timestamps for " + tickerSymbol + ".");
        }

        try {
            return new MarketQuote(
                    tickerSymbol,
                    firstAvailableName(quote, tickerSymbol),
                    quote.getRegularMarketPrice(),
                    quote.getCurrency(),
                    quote.getRegularMarketChangePercent(),
                    OffsetDateTime.parse(quote.getRegularMarketTime()).toInstant(),
                    quote.getFetchedAt(),
                    "brapi.dev"
            );
        } catch (DateTimeParseException ex) {
            throw new MarketDataUnavailableException("Provider returned an invalid market timestamp for " + tickerSymbol + ".", ex);
        }
    }

    private String firstAvailableName(BrapiQuoteDto quote, String tickerSymbol) {
        if (quote.getLongName() != null && !quote.getLongName().isBlank()) return quote.getLongName();
        if (quote.getShortName() != null && !quote.getShortName().isBlank()) return quote.getShortName();
        return tickerSymbol;
    }
}
