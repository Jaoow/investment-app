package dev.jaoow.investmentapp.application.service;

import dev.jaoow.cotatrack.api.QuoteQuery;
import dev.jaoow.cotatrack.api.requests.quotes.model.Quote;
import dev.jaoow.investmentapp.application.exception.MarketDataUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Optional;

/**
 * Serviço de cotações que delega ao motor multi-provedor do CotaTrack
 * (TradingView → Yahoo Chart → Brapi).
 */
@Slf4j
@Service
public class QuoteService {

    public Quote getQuote(String ticker) {
        try {
            Quote quote = QuoteQuery.builder()
                    .symbol(ticker)
                    .build()
                    .fetch();
            if (quote == null) {
                throw new MarketDataUnavailableException("No quote was returned for " + ticker + ".");
            }
            return quote;
        } catch (IOException ex) {
            throw new MarketDataUnavailableException(
                    "Market data provider request failed for " + ticker + ".", ex);
        }
    }

    public Optional<Quote> findQuote(String ticker) {
        try {
            Quote quote = QuoteQuery.builder()
                    .symbol(ticker)
                    .build()
                    .fetch();
            return Optional.ofNullable(quote);
        } catch (Exception ex) {
            log.warn("Failed to fetch quote for {}: {}", ticker, ex.getMessage());
            return Optional.empty();
        }
    }
}
