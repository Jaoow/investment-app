package dev.jaoow.investmentapp.application.exception;

public class InvalidTickerException extends RuntimeException {
    public InvalidTickerException(String message) {
        super(message);
    }
}
