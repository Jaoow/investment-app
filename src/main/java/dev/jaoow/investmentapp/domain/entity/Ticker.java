package dev.jaoow.investmentapp.domain.entity;

import dev.jaoow.investmentapp.domain.model.AssetCategory;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import lombok.Data;
import java.math.BigDecimal;

@Entity
@Data
public class Ticker {
    @Id
    private String symbol;

    @Enumerated(EnumType.STRING)
    private AssetCategory category;

    private String sector;

    private String subSector;

    private BigDecimal priceCeiling;
}
