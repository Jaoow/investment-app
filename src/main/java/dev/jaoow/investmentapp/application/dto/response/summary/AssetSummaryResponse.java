package dev.jaoow.investmentapp.application.dto.response.summary;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import dev.jaoow.investmentapp.application.model.BrapiFields;
import dev.jaoow.investmentapp.application.model.TickerFields;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class AssetSummaryResponse {

    private String tickerSymbol;
    private BigDecimal quantity;
    private BigDecimal averagePrice;
    private BigDecimal totalInvested;
    private BigDecimal currentValue;
    private BigDecimal profitOrLoss;
    private BigDecimal percentageChange;

    @JsonProperty("quoteData")
    private BrapiFields brapiFields;

    @JsonProperty("tickerData")
    private TickerFields tickerFields;
}
