package dev.jaoow.investmentapp.application.service;

import dev.jaoow.investmentapp.application.exception.MarketDataUnavailableException;
import dev.jaoow.investmentapp.application.model.MarketQuote;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MarketQuoteLoaderTests {

    private static final Instant NOW = Instant.parse("2026-10-02T12:00:00Z");

    private final MarketQuoteProvider provider = mock(MarketQuoteProvider.class);
    private final MarketQuoteLoader loader = new MarketQuoteLoader(provider,
            new MarketQuoteFreshness(Clock.fixed(NOW, ZoneOffset.UTC), Duration.ofMinutes(15)));

    @Test
    void fetchesDistinctSymbolsWithASingleBatchRequest() {
        when(provider.getQuotes(List.of("ABC", "DEF"))).thenReturn(Map.of(
                "ABC", quote("ABC", NOW), "DEF", quote("DEF", NOW)));

        Map<String, MarketQuote> quotes = loader.loadFresh(List.of("DEF", "ABC", "DEF"));

        assertEquals(Set.of("ABC", "DEF"), quotes.keySet());
        verify(provider, times(1)).getQuotes(List.of("ABC", "DEF"));
        verify(provider, never()).getQuote(anyString());
    }

    @Test
    void failsWhenTheBatchOmitsARequestedSymbol() {
        when(provider.getQuotes(List.of("ABC", "DEF"))).thenReturn(Map.of("ABC", quote("ABC", NOW)));

        assertThrows(MarketDataUnavailableException.class, () -> loader.loadFresh(Set.of("ABC", "DEF")));
    }

    @Test
    void rejectsStaleQuotesReturnedByTheBatch() {
        when(provider.getQuotes(List.of("ABC"))).thenReturn(Map.of("ABC", quote("ABC", NOW.minusSeconds(3600))));

        assertThrows(MarketDataUnavailableException.class, () -> loader.loadFresh(List.of("ABC")));
    }

    @Test
    void skipsTheProviderWhenNothingIsRequested() {
        assertEquals(Map.of(), loader.loadFresh(List.of()));
        verify(provider, never()).getQuotes(org.mockito.ArgumentMatchers.anyList());
    }

    private MarketQuote quote(String ticker, Instant observedAt) {
        return new MarketQuote(ticker, ticker, new BigDecimal("10"), "BRL", BigDecimal.ZERO, observedAt, observedAt, "test");
    }
}
