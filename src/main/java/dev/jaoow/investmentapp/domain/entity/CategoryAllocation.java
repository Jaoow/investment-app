package dev.jaoow.investmentapp.domain.entity;

import dev.jaoow.investmentapp.domain.model.AssetCategory;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"portfolio_id", "category"}))
@Data
public class CategoryAllocation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssetCategory category;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal targetPercentage;

    @OneToMany(mappedBy = "categoryAllocation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AssetAllocation> assetAllocations;
}
