package dev.jaoow.investmentapp.application.exception;

public class InvalidPortfolioShareException extends RuntimeException {
    public InvalidPortfolioShareException(String message) {
        super(message);
    }
}
