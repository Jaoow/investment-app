package dev.jaoow.investmentapp.application.service;

import dev.jaoow.investmentapp.application.exception.MarketDataUnavailableException;
import dev.jaoow.investmentapp.application.model.MarketQuote;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Loads every quote a use case needs with one batch request and enforces freshness on each of them. */
@Slf4j
@Component
public class MarketQuoteLoader {

    private final MarketQuoteProvider marketQuoteProvider;
    private final MarketQuoteFreshness marketQuoteFreshness;

    public MarketQuoteLoader(MarketQuoteProvider marketQuoteProvider, MarketQuoteFreshness marketQuoteFreshness) {
        this.marketQuoteProvider = marketQuoteProvider;
        this.marketQuoteFreshness = marketQuoteFreshness;
    }

    public Map<String, MarketQuote> loadFresh(Collection<String> tickerSymbols) {
        List<String> symbols = tickerSymbols.stream().filter(Objects::nonNull).distinct().sorted().toList();
        if (symbols.isEmpty()) return Map.of();

        long startedAt = System.nanoTime();
        Map<String, MarketQuote> fetched = marketQuoteProvider.getQuotes(symbols);
        Map<String, MarketQuote> quotes = new LinkedHashMap<>();
        for (String symbol : symbols) {
            MarketQuote quote = fetched == null ? null : fetched.get(symbol);
            if (quote == null) {
                throw new MarketDataUnavailableException("Quote not found in provider response for " + symbol + ".");
            }
            quotes.put(symbol, marketQuoteFreshness.requireFresh(quote));
        }
        log.info("Market quotes loaded in batch: symbols={} durationMs={}",
                symbols.size(), (System.nanoTime() - startedAt) / 1_000_000);
        return quotes;
    }
}
