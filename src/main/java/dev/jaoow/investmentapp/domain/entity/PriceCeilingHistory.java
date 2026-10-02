package dev.jaoow.investmentapp.domain.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Data
public class PriceCeilingHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long portfolioId;

    @Column(nullable = false)
    private String tickerSymbol;

    @Column(precision = 19, scale = 4)
    private BigDecimal previousPrice;

    @Column(precision = 19, scale = 4)
    private BigDecimal priceCeiling;

    @Column(nullable = false, length = 16)
    private String action;

    @Column(nullable = false)
    private Instant changedAt;
}
