package dev.jaoow.investmentapp.application.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class BrapiTickerListResponse {
    private List<BrapiTickerListItemDto> results;
}
