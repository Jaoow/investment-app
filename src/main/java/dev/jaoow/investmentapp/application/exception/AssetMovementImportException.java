package dev.jaoow.investmentapp.application.exception;

public class AssetMovementImportException extends RuntimeException {

    public AssetMovementImportException() {
        super("Error importing asset movements");
    }
}
