package dev.jaoow.investmentapp.application.dto.response;

import lombok.Data;
import java.time.Instant;

@Data
public class BrapiTickerResolveResultDto {
    private String requestedSymbol;
    private String symbol;
    private boolean changed;
    private String status;
    private Instant effectiveDate;
    private Double conversionRatio;
}
