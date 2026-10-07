package dev.jaoow.investmentapp.application.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class B3ImportDetailResponse {
    private B3ImportResultResponse importSummary;
}
