package dev.jaoow.investmentapp.application.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class PortfolioResponse {
    private Long id;
    private String name;
    private Integer totalAssets;
}
