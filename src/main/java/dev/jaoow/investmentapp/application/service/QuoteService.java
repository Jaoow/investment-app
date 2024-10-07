package dev.jaoow.investmentapp.application.service;

import dev.jaoow.investmentapp.application.dto.response.BrapiQuoteDto;
import dev.jaoow.investmentapp.infrastructure.client.BrapiClient;
import org.springframework.stereotype.Service;

@Service
public class QuoteService {

    private final BrapiClient brapiClient;

    public QuoteService(BrapiClient brapiClient) {
        this.brapiClient = brapiClient;
    }

    public BrapiQuoteDto getQuote(String ticker, String range, String interval, Boolean fundamental, Boolean dividends) {
        return brapiClient.getQuote(ticker, range, interval, fundamental, dividends).orElseThrow(() -> new RuntimeException("Quote not found"));
    }
}
