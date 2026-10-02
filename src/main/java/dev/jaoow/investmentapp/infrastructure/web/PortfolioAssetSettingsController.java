package dev.jaoow.investmentapp.infrastructure.web;

import dev.jaoow.investmentapp.application.dto.response.PortfolioAssetSettingResponse;
import dev.jaoow.investmentapp.application.service.PortfolioAssetPreferenceService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/portfolio/{portfolioId}/asset-settings")
public class PortfolioAssetSettingsController {
    private final PortfolioAssetPreferenceService preferenceService;

    public PortfolioAssetSettingsController(PortfolioAssetPreferenceService preferenceService) {
        this.preferenceService = preferenceService;
    }

    @GetMapping
    public List<PortfolioAssetSettingResponse> getSettings(@PathVariable Long portfolioId) {
        return preferenceService.getSettings(portfolioId);
    }
}
