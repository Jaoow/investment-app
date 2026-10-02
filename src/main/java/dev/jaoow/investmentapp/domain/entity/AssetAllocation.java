package dev.jaoow.investmentapp.domain.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Entity
@Data
public class AssetAllocation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String tickerSymbol;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal targetPercentage;

    @ManyToOne
    @JoinColumn(name = "category_allocation_id", nullable = false)
    private CategoryAllocation categoryAllocation;
}
