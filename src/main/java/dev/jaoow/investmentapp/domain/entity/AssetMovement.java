package dev.jaoow.investmentapp.domain.entity;

import dev.jaoow.investmentapp.domain.model.MovementType;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Data
public class AssetMovement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "portfolio_id")
    private Portfolio portfolio;

    private String tickerSymbol;

    private BigDecimal quantity;

    private BigDecimal price;

    @Temporal(TemporalType.DATE)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    private MovementType type;
    
    @Column(name = "external_reference", unique = true)
    private String externalReference;
}
