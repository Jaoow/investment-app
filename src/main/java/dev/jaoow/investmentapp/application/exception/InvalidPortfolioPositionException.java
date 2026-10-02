package dev.jaoow.investmentapp.application.exception;

public class InvalidPortfolioPositionException extends RuntimeException {
    public InvalidPortfolioPositionException(String message) {
        super(message);
    }
}
