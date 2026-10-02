package dev.jaoow.investmentapp.infrastructure.web;

import dev.jaoow.investmentapp.application.dto.request.PortfolioAssetPreferenceRequest;
import dev.jaoow.investmentapp.application.dto.response.PortfolioAssetPreferenceResponse;
import dev.jaoow.investmentapp.application.dto.response.PriceCeilingHistoryResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import dev.jaoow.investmentapp.application.service.PortfolioAssetPreferenceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/portfolio/{portfolioId}/assets/{tickerSymbol}/ceiling")
public class PortfolioAssetPreferenceController {

    private final PortfolioAssetPreferenceService preferenceService;

    public PortfolioAssetPreferenceController(PortfolioAssetPreferenceService preferenceService) {
        this.preferenceService = preferenceService;
    }

    @GetMapping
    public PortfolioAssetPreferenceResponse getPreference(
            @PathVariable Long portfolioId,
            @PathVariable String tickerSymbol) {
        return preferenceService.getPreference(portfolioId, tickerSymbol);
    }

    @PutMapping
    public PortfolioAssetPreferenceResponse setPreference(
            @PathVariable Long portfolioId,
            @PathVariable String tickerSymbol,
            @Valid @RequestBody PortfolioAssetPreferenceRequest request) {
        return preferenceService.setPreference(portfolioId, tickerSymbol, request);
    }

    @GetMapping("/history")
    public PagedModel<PriceCeilingHistoryResponse> getHistory(
            @PathVariable Long portfolioId, @PathVariable String tickerSymbol, Pageable pageable) {
        return preferenceService.getHistory(portfolioId, tickerSymbol, pageable);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePreference(@PathVariable Long portfolioId, @PathVariable String tickerSymbol) {
        preferenceService.deletePreference(portfolioId, tickerSymbol);
    }
}
