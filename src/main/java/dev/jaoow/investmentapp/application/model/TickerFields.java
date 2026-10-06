package dev.jaoow.investmentapp.application.model;

import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
public class TickerFields {
    private String category;
    private String sector;
    private String subSector;
}
