package dev.jaoow.investmentapp.application.dto.response.summary;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class PortfolioSummaryResponse {
    private Long portfolioId;
    private BigDecimal totalInvested;
    private BigDecimal currentValue;
    private BigDecimal profitOrLoss;
    private BigDecimal percentageChange;
    private List<AssetSummaryResponse> assetSummaries;
}
