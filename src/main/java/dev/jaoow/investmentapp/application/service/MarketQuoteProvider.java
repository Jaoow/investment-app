package dev.jaoow.investmentapp.application.service;

import dev.jaoow.investmentapp.application.model.MarketQuote;

public interface MarketQuoteProvider {
    MarketQuote getQuote(String tickerSymbol);
}
