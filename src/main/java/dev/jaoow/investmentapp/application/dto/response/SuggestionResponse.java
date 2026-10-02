package dev.jaoow.investmentapp.application.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class SuggestionResponse {
    private BigDecimal requestedAmount;
    private BigDecimal allocatedAmount;
    private BigDecimal remainingAmount;
    private String currency;
    private List<SuggestionItemResponse> items;
    private List<ExcludedSuggestionResponse> excludedAssets;
    private String disclaimer;
}
