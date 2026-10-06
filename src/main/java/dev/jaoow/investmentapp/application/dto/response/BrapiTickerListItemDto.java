package dev.jaoow.investmentapp.application.dto.response;

import lombok.Data;

@Data
public class BrapiTickerListItemDto {
    private String symbol;
    private String name;
    private String longName;
    private String assetType;
    private String subType;
    private String sector;
    private String subsector;
    private String logoUrl;
}
