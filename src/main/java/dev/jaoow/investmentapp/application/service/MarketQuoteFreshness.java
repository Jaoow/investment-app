package dev.jaoow.investmentapp.application.service;

import dev.jaoow.investmentapp.application.exception.MarketDataUnavailableException;
import dev.jaoow.investmentapp.application.model.MarketQuote;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Component
public class MarketQuoteFreshness {

    private final Clock clock;
    private final Duration maximumAge;

    public MarketQuoteFreshness(
            Clock clock,
            @Value("${investize.recommendations.max-quote-age:15m}") Duration maximumAge) {
        if (maximumAge.isNegative() || maximumAge.isZero()) {
            throw new IllegalArgumentException("Maximum market quote age must be positive.");
        }
        this.clock = clock;
        this.maximumAge = maximumAge;
    }

    public MarketQuote requireFresh(MarketQuote quote) {
        Instant now = clock.instant();
        if (quote.observedAt() == null || quote.fetchedAt() == null) {
            throw new MarketDataUnavailableException("Quote timestamps are required for " + quote.tickerSymbol() + ".");
        }
        if (quote.observedAt().isAfter(now) || quote.fetchedAt().isAfter(now)) {
            throw new MarketDataUnavailableException("Provider returned a future quote timestamp for "
                    + quote.tickerSymbol() + ".");
        }
        if (Duration.between(quote.observedAt(), now).compareTo(maximumAge) > 0) {
            throw new MarketDataUnavailableException("Market quote for " + quote.tickerSymbol()
                    + " is older than the configured maximum age.");
        }
        return quote;
    }
}
