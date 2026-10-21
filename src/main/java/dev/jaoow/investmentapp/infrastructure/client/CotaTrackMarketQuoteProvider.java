package dev.jaoow.investmentapp.infrastructure.client;

import dev.jaoow.cotatrack.api.provider.ProviderResult;
import dev.jaoow.cotatrack.api.provider.orchestrator.ProviderOrchestrator;
import dev.jaoow.cotatrack.api.provider.orchestrator.ProviderOrchestratorHolder;
import dev.jaoow.cotatrack.api.requests.quotes.model.Quote;
import dev.jaoow.cotatrack.api.yahoo.SymbolMapper;
import dev.jaoow.investmentapp.application.exception.MarketDataUnavailableException;
import dev.jaoow.investmentapp.application.model.MarketQuote;
import dev.jaoow.investmentapp.application.service.MarketQuoteProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Implementação de {@link MarketQuoteProvider} que delega ao motor de cotações
 * multi-provedor do CotaTrack.
 *
 * <p>Cadeia de provedores (com circuit breakers e rate limiters):
 * <ol>
 *   <li>TradingView Scanner (primário – batch, sem API key)</li>
 *   <li>Yahoo Finance Chart v8 (fallback)</li>
 *   <li>brapi.dev (fallback terciário)</li>
 *   <li>Stale Cache 24h (disaster recovery)</li>
 * </ol>
 */
@Slf4j
@Component
public class CotaTrackMarketQuoteProvider implements MarketQuoteProvider {

    private final ProviderOrchestrator orchestrator;

    public CotaTrackMarketQuoteProvider() {
        this.orchestrator = ProviderOrchestratorHolder.getOrchestrator();
    }

    @Override
    public MarketQuote getQuote(String tickerSymbol) {
        return getQuotes(List.of(tickerSymbol)).get(tickerSymbol);
    }

    @Override
    public Map<String, MarketQuote> getQuotes(List<String> tickerSymbols) {
        if (tickerSymbols == null || tickerSymbols.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, String> requestedSymbols = tickerSymbols.stream()
                .distinct()
                .collect(Collectors.toMap(Function.identity(), SymbolMapper::mapSymbol));

        ProviderResult<java.util.Map<String, Quote>> result;
        try {
            result = orchestrator.fetchQuotes(List.copyOf(requestedSymbols.values()));
        } catch (Exception ex) {
            throw new MarketDataUnavailableException(
                    "CotaTrack orchestrator failed while fetching market quotes: " + ex.getMessage(), ex);
        }

        if (result == null || result.getData() == null || result.getData().isEmpty()) {
            throw new MarketDataUnavailableException(
                    "All market data providers returned no market quotes.");
        }

        Map<String, MarketQuote> quotes = new java.util.HashMap<>();
        for (Map.Entry<String, String> requested : requestedSymbols.entrySet()) {
            Quote quote = findQuote(result.getData(), requested.getValue());
            if (quote == null) {
                quote = findQuote(result.getData(), requested.getKey());
            }
            if (quote == null) {
                throw new MarketDataUnavailableException(
                        "Quote not found in provider response for " + requested.getKey() + ".");
            }
            quotes.put(requested.getKey(), mapToMarketQuote(quote, requested.getKey(), result.getProviderName()));
        }
        return quotes;
    }

    private Quote findQuote(java.util.Map<String, Quote> data, String symbol) {
        if (data.containsKey(symbol)) return data.get(symbol);
        String clean = symbol.replace(".SA", "");
        if (data.containsKey(clean)) return data.get(clean);
        if (data.containsKey(clean + ".SA")) return data.get(clean + ".SA");
        // Case-insensitive fallback
        return data.entrySet().stream()
                .filter(e -> e.getKey().equalsIgnoreCase(symbol) ||
                             e.getKey().equalsIgnoreCase(clean))
                .map(java.util.Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    private MarketQuote mapToMarketQuote(Quote quote, String originalSymbol, String providerName) {
        if (quote.getRegularMarketPrice() == null || quote.getRegularMarketPrice() <= 0) {
            throw new MarketDataUnavailableException(
                    "Provider returned no valid price for " + originalSymbol + ".");
        }
        if (quote.getCurrency() == null || quote.getCurrency().isBlank()) {
            throw new MarketDataUnavailableException(
                    "Provider returned no currency for " + originalSymbol + ".");
        }

        // regularMarketTime from CotaTrack is a java.util.Date
        Instant observedAt = quote.getRegularMarketTime() != null
                ? quote.getRegularMarketTime().toInstant()
                : null;

        if (observedAt == null) {
            throw new MarketDataUnavailableException(
                    "Provider omitted market timestamp for " + originalSymbol + ".");
        }

        String assetName = resolveAssetName(quote, originalSymbol);
        BigDecimal price = BigDecimal.valueOf(quote.getRegularMarketPrice());
        BigDecimal changePercent = quote.getRegularMarketChangePercent() != null
                ? BigDecimal.valueOf(quote.getRegularMarketChangePercent())
                : null;

        log.debug("CotaTrack quote for {}: price={} via provider={} (stale={})",
                originalSymbol, price, providerName, false);

        return new MarketQuote(
                originalSymbol,
                assetName,
                price,
                quote.getCurrency(),
                changePercent,
                observedAt,
                Instant.now(),
                providerName
        );
    }

    private String resolveAssetName(Quote quote, String fallback) {
        if (quote.getLongName() != null && !quote.getLongName().isBlank()) return quote.getLongName();
        if (quote.getShortName() != null && !quote.getShortName().isBlank()) return quote.getShortName();
        if (quote.getSymbol() != null && !quote.getSymbol().isBlank()) return quote.getSymbol();
        return fallback;
    }
}
