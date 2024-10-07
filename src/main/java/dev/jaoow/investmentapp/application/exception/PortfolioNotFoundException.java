package dev.jaoow.investmentapp.application.exception;

public class PortfolioNotFoundException extends RuntimeException {
    public PortfolioNotFoundException(Long portfolioId) {
        super("Portfolio with ID " + portfolioId + " not found");
    }
}