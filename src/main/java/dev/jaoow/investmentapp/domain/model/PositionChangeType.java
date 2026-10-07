package dev.jaoow.investmentapp.domain.model;

/**
 * Represents the direction of a position change detected between two B3 snapshots.
 * This is intentionally generic to allow future evolution into specific operation types
 * (purchase, sale, bonus shares, split, reverse split, subscription, etc.).
 */
public enum PositionChangeType {
    /** Asset appeared in the new snapshot but not in the previous one */
    NEW_POSITION,
    /** Asset exists in the previous snapshot but is gone from the new one */
    POSITION_CLOSED,
    /** The quantity in the new snapshot is greater than in the previous one */
    QUANTITY_INCREASED,
    /** The quantity in the new snapshot is smaller than in the previous one */
    QUANTITY_DECREASED,
    /** The quantity is the same in both snapshots — no change detected */
    NO_CHANGE
}
