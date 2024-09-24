package dev.jaoow.investmentapp.application.dto.request;

import dev.jaoow.investmentapp.domain.model.AssetCategory;
import lombok.*;

@Data
@NoArgsConstructor
public class TickerFilterRequest {

    private String sector;
    private String subSector;
    private AssetCategory category;
    private String symbol;

}
