package dev.jaoow.investmentapp.domain.model;

/**
 * Represents the status of a B3 position import operation.
 */
public enum B3ImportStatus {
    /** Import was processed successfully */
    SUCCESS,
    /** Import completed but with warnings (some items were skipped or had issues) */
    PARTIAL,
    /** Import failed due to a critical error */
    FAILED
}
