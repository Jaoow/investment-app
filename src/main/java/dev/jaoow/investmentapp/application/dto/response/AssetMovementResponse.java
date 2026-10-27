package dev.jaoow.investmentapp.application.dto.response;

import dev.jaoow.investmentapp.domain.model.MovementType;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class AssetMovementResponse {
    private Long id;
    private Long portfolioId;
    private String tickerSymbol;
    private BigDecimal quantity;
    private BigDecimal price;
    private BigDecimal operationCost;
    private LocalDate date;
    private MovementType type;
    private BigDecimal adjustmentQuantity;
    private BigDecimal adjustmentTotalCost;
    private BigDecimal previousQuantity;
    private BigDecimal previousTotalCost;
    private String reason;
    private String adjustmentAuthor;
    private java.time.Instant adjustmentRecordedAt;
}
