package dev.jaoow.investmentapp.application.service;

import dev.jaoow.investmentapp.application.model.MarketTicker;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MarketTickerService {

    private final MarketTickerProvider marketTickerProvider;

    public MarketTickerService(MarketTickerProvider marketTickerProvider) {
        this.marketTickerProvider = marketTickerProvider;
    }

    public List<MarketTicker> searchTickers(String query, String type) {
        return marketTickerProvider.searchTickers(query, type);
    }
}
