package dev.jaoow.investmentapp.application.service;

import dev.jaoow.investmentapp.application.model.MarketTicker;
import java.util.List;

public interface MarketTickerProvider {
    List<MarketTicker> searchTickers(String query, String type);
}
