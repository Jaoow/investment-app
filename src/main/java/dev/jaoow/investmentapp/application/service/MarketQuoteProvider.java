package dev.jaoow.investmentapp.application.service;

import dev.jaoow.investmentapp.application.model.MarketQuote;

import java.util.List;
import java.util.Map;

public interface MarketQuoteProvider {
    MarketQuote getQuote(String tickerSymbol);

    default Map<String, MarketQuote> getQuotes(List<String> tickerSymbols) {
        return tickerSymbols.stream()
                .collect(java.util.stream.Collectors.toMap(
                        ticker -> ticker,
                        this::getQuote
                ));
    }
}
