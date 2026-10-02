package dev.jaoow.investmentapp.application.dto.request;

import dev.jaoow.investmentapp.domain.model.AssetCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterTickerRequest {
    @NotBlank
    @Size(max = 12)
    private String symbol;

    @NotNull
    private AssetCategory category;
}
