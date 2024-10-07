package dev.jaoow.investmentapp.application.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class TickerFields {
    private String category;
    private String sector;
    private String subSector;
    private BigDecimal priceCeiling;
}
