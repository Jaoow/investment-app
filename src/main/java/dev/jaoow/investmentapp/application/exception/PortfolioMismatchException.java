package dev.jaoow.investmentapp.application.exception;

public class PortfolioMismatchException extends RuntimeException {
    public PortfolioMismatchException(Long portfolioId, Long movementId) {
        super("Asset Movement with ID " + movementId + " does not belong to Portfolio with ID " + portfolioId);
    }
}