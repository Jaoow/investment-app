package dev.jaoow.investmentapp.application.exception;

public class PositionConflictException extends RuntimeException {
    public PositionConflictException(String message) {
        super(message);
    }
}
