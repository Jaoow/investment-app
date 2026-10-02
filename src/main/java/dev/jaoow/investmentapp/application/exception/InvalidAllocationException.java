package dev.jaoow.investmentapp.application.exception;

public class InvalidAllocationException extends RuntimeException {
    public InvalidAllocationException(String message) {
        super(message);
    }
}
