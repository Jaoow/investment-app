package dev.jaoow.investmentapp.application.dto.response;

import dev.jaoow.investmentapp.domain.model.AssetCategory;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CategoryAllocationResponse {
    private AssetCategory category;
    private BigDecimal categoryTargetPercentage;
    private List<AssetAllocationResponse> assetAllocations;

    @Data
    public static class AssetAllocationResponse {
        private String tickerSymbol;
        private BigDecimal targetPercentage;
    }
}
