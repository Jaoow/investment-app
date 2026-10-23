package dev.jaoow.investmentapp.application.service;

import dev.jaoow.cotatrack.api.QuoteQuery;
import dev.jaoow.cotatrack.api.requests.quotes.model.Quote;
import dev.jaoow.cotatrack.api.yahoo.SymbolMapper;
import dev.jaoow.investmentapp.application.exception.MarketDataUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
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

    /**
     * Fetches all quotes in a single batch request. Lenient like {@link #findQuote(String)}:
     * symbols the providers cannot resolve are simply absent from the returned map.
     */
    public Map<String, Quote> findQuotes(Collection<String> tickers) {
        List<String> symbols = tickers.stream().filter(Objects::nonNull).distinct().toList();
        if (symbols.isEmpty()) return Map.of();

        long startedAt = System.nanoTime();
        Map<String, Quote> fetched;
        try {
            fetched = QuoteQuery.builder()
                    .symbols(symbols.toArray(String[]::new))
                    .build()
                    .fetchMultiple();
        } catch (Exception ex) {
            log.warn("Batch quote fetch failed: requested={} durationMs={} error={}",
                    symbols.size(), elapsedMillis(startedAt), ex.getMessage());
            return Map.of();
        }

        Map<String, Quote> quotes = new HashMap<>();
        if (fetched != null) {
            for (String symbol : symbols) {
                Quote quote = match(fetched, symbol);
                if (quote != null) quotes.put(symbol, quote);
            }
        }
        log.info("Batch quote fetch: requested={} resolved={} missing={} durationMs={}",
                symbols.size(), quotes.size(), symbols.size() - quotes.size(), elapsedMillis(startedAt));
        return quotes;
    }

    private static Quote match(Map<String, Quote> fetched, String symbol) {
        String mapped = SymbolMapper.mapSymbol(symbol).toUpperCase(Locale.ROOT);
        String clean = symbol.toUpperCase(Locale.ROOT).replace(".SA", "");
        for (String candidate : List.of(mapped, symbol, clean, clean + ".SA")) {
            Quote quote = fetched.get(candidate);
            if (quote != null) return quote;
        }
        return fetched.entrySet().stream()
                .filter(entry -> entry.getKey() != null
                        && (entry.getKey().equalsIgnoreCase(mapped) || entry.getKey().equalsIgnoreCase(clean)))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    private static long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }
}
