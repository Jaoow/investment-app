package dev.jaoow.investmentapp.domain.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Entity
@Table(
        name = "portfolio_asset_preference",
        uniqueConstraints = @UniqueConstraint(columnNames = {"portfolio_id", "ticker_symbol"})
)
@Data
public class PortfolioAssetPreference {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;

    @Column(nullable = false)
    private String tickerSymbol;

    @Column(precision = 19, scale = 4)
    private BigDecimal priceCeiling;

    @Column(name = "contribution_enabled", nullable = false)
    private boolean contributionEnabled = true;
}
