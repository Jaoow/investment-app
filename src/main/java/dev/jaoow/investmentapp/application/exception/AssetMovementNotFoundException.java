package dev.jaoow.investmentapp.application.exception;

public class AssetMovementNotFoundException extends RuntimeException {
    public AssetMovementNotFoundException(Long movementId) {
        super("Asset Movement with ID " + movementId + " not found");
    }
}