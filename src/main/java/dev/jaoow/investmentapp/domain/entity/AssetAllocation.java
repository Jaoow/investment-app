package dev.jaoow.investmentapp.domain.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class AssetAllocation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "category_allocation_id")
    private CategoryAllocation categoryAllocation;

    private String tickerSymbol;

    private double targetPercentage;
}
