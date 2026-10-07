package dev.jaoow.investmentapp.application.dto.response;

import dev.jaoow.investmentapp.domain.model.B3ImportStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class B3ImportResultResponse {
    private Long id;
    private Long portfolioId;
    private String fileName;
    private Instant importedAt;
    private B3ImportStatus status;
    private int totalNegotiationsFound;
    private int newNegotiations;
    private int alreadyImported;
    private int inconsistencies;
    private String errorMessage;
}
