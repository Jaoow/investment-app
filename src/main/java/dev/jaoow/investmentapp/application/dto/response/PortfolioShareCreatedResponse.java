package dev.jaoow.investmentapp.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class PortfolioShareCreatedResponse {
    private Long id;
    private String accessPath;
    private Instant expiresAt;
    private boolean includeValues;
}
