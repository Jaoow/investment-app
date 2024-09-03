package dev.jaoow.investmentapp.application.dto.request;

import dev.jaoow.investmentapp.domain.model.AssetCategory;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class TickerRequest {
    @NotBlank(message = "Symbol is mandatory")
    private String symbol;

    @NotNull(message = "Category is mandatory")
    private AssetCategory category;

    @NotBlank(message = "Sector is mandatory")
    private String sector;

    @NotBlank(message = "SubSector is mandatory")
    private String subSector;

    @NotNull(message = "Price ceiling is mandatory")
    @Positive(message = "Price ceiling must be positive")
    private BigDecimal priceCeiling;
}
