package dev.jaoow.investmentapp.application.exception;

public class AssetMovementImportException extends RuntimeException {

    public AssetMovementImportException() {
        this("Error importing asset movements", null);
    }

    public AssetMovementImportException(String message, Throwable cause) {
        super(message, cause);
    }
}
