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
    private String industry;
    private String category;
    private String logoUrl;

    public MarketTicker(String symbol, String name, String longName, String type, String subType, String sector, String subSector, String logoUrl) {
        this(symbol, name, longName, type, subType, sector, subSector, null, null, logoUrl);
    }
}
