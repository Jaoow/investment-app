package dev.jaoow.investmentapp.application.dto.response;

import lombok.Data;
import java.util.List;
import java.time.Instant;

@Data
public class BrapiTickerResolveResponseDto {
    private List<BrapiTickerResolveResultDto> results;
    private Instant requestedAt;
    private Integer took;
}
