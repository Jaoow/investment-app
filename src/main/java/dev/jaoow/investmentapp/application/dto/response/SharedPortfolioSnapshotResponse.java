package dev.jaoow.investmentapp.application.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
public class SharedPortfolioSnapshotResponse {
    private Instant generatedAt;
    private Instant expiresAt;
    private boolean includesValues;
    private List<SharedAssetResponse> assets;

    @Data
    public static class SharedAssetResponse {
        private String tickerSymbol;
        private BigDecimal quantity;
        private BigDecimal unitPrice;
        private BigDecimal marketValue;
        private String currency;
        private String quoteProvider;
        private Instant quoteObservedAt;
    }
}
