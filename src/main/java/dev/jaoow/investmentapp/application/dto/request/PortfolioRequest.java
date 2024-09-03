package dev.jaoow.investmentapp.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PortfolioRequest {
    @NotBlank(message = "Name is mandatory")
    private String name;
}
