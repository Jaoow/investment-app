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
    private LocalDate date;
    private MovementType type;
}
