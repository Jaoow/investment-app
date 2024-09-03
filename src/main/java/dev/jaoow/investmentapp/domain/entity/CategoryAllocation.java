package dev.jaoow.investmentapp.domain.entity;

import dev.jaoow.investmentapp.domain.model.AssetCategory;
import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Data
public class CategoryAllocation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "portfolio_id")
    private Portfolio portfolio;

    @Enumerated(EnumType.STRING)
    private AssetCategory category;

    private double targetPercentage;

    @OneToMany(mappedBy = "categoryAllocation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AssetAllocation> assetAllocations;
}
