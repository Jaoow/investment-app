package dev.jaoow.investmentapp.application.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MarketTicker {
    private String symbol;
    private String name;
    private String longName;
    private String type;
    private String subType;
    private String sector;
    private String subSector;
    private String logoUrl;
}
