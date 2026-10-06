package dev.jaoow.investmentapp.application.dto.response;

import lombok.Data;


@Data
public class TickerResponse {
    private String symbol;
    private String category;
    private String sector;
    private String subSector;
    private String name;
    private String logoUrl;
}
