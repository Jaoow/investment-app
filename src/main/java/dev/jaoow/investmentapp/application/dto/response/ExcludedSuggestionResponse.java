package dev.jaoow.investmentapp.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ExcludedSuggestionResponse {
    private String tickerSymbol;
    private String reason;
}
