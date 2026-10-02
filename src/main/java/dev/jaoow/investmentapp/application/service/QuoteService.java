package dev.jaoow.investmentapp.application.service;

import dev.jaoow.investmentapp.application.dto.response.BrapiQuoteDto;
import dev.jaoow.investmentapp.application.exception.MarketDataUnavailableException;
import dev.jaoow.investmentapp.infrastructure.client.BrapiClient;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

@Service
public class QuoteService {

    private final BrapiClient brapiClient;

    public QuoteService(BrapiClient brapiClient) {
        this.brapiClient = brapiClient;
    }

    public BrapiQuoteDto getQuote(String ticker, String range, String interval, Boolean fundamental, Boolean dividends) {
        try {
            return brapiClient.getQuote(ticker, range, interval, fundamental, dividends)
                    .orElseThrow(() -> new MarketDataUnavailableException("No quote was returned for " + ticker + "."));
        } catch (RestClientException ex) {
            throw new MarketDataUnavailableException("Market data provider request failed for " + ticker + ".", ex);
        }
    }
}
