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

    private BigDecimal operationCost;

    @Temporal(TemporalType.DATE)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    private MovementType type;
    
    @Column(name = "external_reference", unique = true)
    private String externalReference;

    @Column(precision = 38, scale = 8)
    private BigDecimal adjustmentQuantity;
    @Column(precision = 38, scale = 8)
    private BigDecimal adjustmentTotalCost;
    @Column(precision = 38, scale = 8)
    private BigDecimal previousQuantity;
    @Column(precision = 64, scale = 16)
    private BigDecimal previousTotalCost;
    @Column(name = "adjustment_reason")
    private String reason;
    private String adjustmentAuthor;
    private java.time.Instant adjustmentRecordedAt;
    private java.util.UUID adjustmentRequestId;
    private Long adjustmentExpectedRevision;
    private Long adjustmentResultRevision;
}
