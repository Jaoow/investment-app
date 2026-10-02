package dev.jaoow.investmentapp.application.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;

@Data
public class CreatePortfolioShareRequest {
    @NotNull
    @Future
    private Instant expiresAt;

    private boolean includeValues;
}
