package dev.jaoow.investmentapp.application.dto.request;

import dev.jaoow.investmentapp.domain.model.MovementType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class AssetMovementRequest {

    @NotNull
    private String tickerSymbol;

    @NotNull
    @Positive(message = "Quantity must be positive")
    private BigDecimal quantity;

    @NotNull
    @Positive(message = "Price must be positive")
    private BigDecimal price;

    @NotNull
    private MovementType type;

    @NotNull
    @PastOrPresent(message = "Date must be in the past or present")
    private LocalDate date;
}
