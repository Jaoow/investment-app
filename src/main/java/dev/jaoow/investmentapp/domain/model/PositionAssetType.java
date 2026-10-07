package dev.jaoow.investmentapp.domain.model;

/**
 * Represents the type/source of an asset in the B3 position snapshot.
 * Used to distinguish between equities, FIIs, fixed income instruments, etc.
 */
public enum PositionAssetType {
    /** Common stocks, preferred stocks, units, BDRs traded on B3 */
    EQUITY,
    /** Real estate investment trusts (FIIs) */
    FII,
    /** Fixed income instruments (CDB, LCI, LCA, CRI, CRA, Debentures) */
    FIXED_INCOME,
    /** Securities lending / BTC positions */
    SECURITIES_LENDING,
    /** Unknown or unsupported asset type */
    OTHER
}
