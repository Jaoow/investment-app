package dev.jaoow.investmentapp.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AssetPositionRequest {

    @NotBlank
    private String tickerSymbol;

    @NotNull
    @Positive(message = "Quantity must be positive")
    private BigDecimal quantity;

    @NotNull
    @Positive(message = "Average price must be positive")
    private BigDecimal averagePrice;
}
