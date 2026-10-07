package dev.jaoow.investmentapp.application.exception;

public class B3ImportException extends RuntimeException {

    public B3ImportException(String message) {
        super(message);
    }

    public B3ImportException(String message, Throwable cause) {
        super(message, cause);
    }
}
