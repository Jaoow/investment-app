package dev.jaoow.investmentapp.application.dto.request;

import dev.jaoow.investmentapp.domain.model.AssetCategory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CategoryAllocationRequest {
    @NotNull
    private AssetCategory category;

    @NotNull
    @DecimalMin("0.00")
    @DecimalMax("100.00")
    @Digits(integer = 3, fraction = 2)
    private BigDecimal categoryTargetPercentage;

    private List<@Valid AssetAllocationRequest> assetAllocations;

    @Data
    public static class AssetAllocationRequest {
        @NotBlank
        private String tickerSymbol;

        @NotNull
        @DecimalMin("0.00")
        @DecimalMax("100.00")
        @Digits(integer = 3, fraction = 2)
        private BigDecimal targetPercentage;
    }
}
