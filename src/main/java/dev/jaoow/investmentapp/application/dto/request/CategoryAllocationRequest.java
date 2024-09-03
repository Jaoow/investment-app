package dev.jaoow.investmentapp.application.dto.request;

import dev.jaoow.investmentapp.domain.model.AssetCategory;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CategoryAllocationRequest {
    @NotNull
    private AssetCategory category;

    @Min(0)
    @Max(100)
    private double categoryTargetPercentage;

    private List<AssetAllocationRequest> assetAllocations;

    @Data
    public static class AssetAllocationRequest {
        @NotNull
        private String tickerSymbol;

        @Min(0)
        @Max(100)
        private double targetPercentage;
    }
}
