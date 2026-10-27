package dev.jaoow.investmentapp.application.dto.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;

public record PositionAdjustmentRequest(
        @NotNull @PositiveOrZero @Digits(integer = 30, fraction = 8) BigDecimal quantity,
        @NotNull @PositiveOrZero @Digits(integer = 30, fraction = 8) BigDecimal totalCost,
        @NotNull @PositiveOrZero Long expectedRevision,
        @NotNull UUID requestId,
        @NotBlank @Size(max = 500) String reason) {
}
