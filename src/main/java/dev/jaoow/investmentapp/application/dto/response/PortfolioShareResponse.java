package dev.jaoow.investmentapp.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class PortfolioShareResponse {
    private Long id;
    private Instant expiresAt;
    private Instant createdAt;
    private Instant revokedAt;
    private boolean includeValues;
}
