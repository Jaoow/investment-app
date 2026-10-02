package dev.jaoow.investmentapp.application.exception;

public class MarketDataUnavailableException extends RuntimeException {
    public MarketDataUnavailableException(String message) {
        super(message);
    }

    public MarketDataUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
