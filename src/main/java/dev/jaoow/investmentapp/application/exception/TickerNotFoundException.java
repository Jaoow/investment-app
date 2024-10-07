package dev.jaoow.investmentapp.application.exception;

public class TickerNotFoundException extends RuntimeException {
    public TickerNotFoundException(String symbol) {
        super("Ticker with symbol '" + symbol + "' not found");
    }
}
