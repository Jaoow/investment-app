package dev.jaoow.investmentapp.infrastructure.web;

import dev.jaoow.investmentapp.application.dto.request.ContributionEligibilityRequest;
import dev.jaoow.investmentapp.application.dto.response.ContributionEligibilityResponse;
import dev.jaoow.investmentapp.application.service.PortfolioAssetPreferenceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/portfolio/{portfolioId}/contribution-eligibility")
public class PortfolioContributionEligibilityController {

    private final PortfolioAssetPreferenceService preferenceService;

    public PortfolioContributionEligibilityController(PortfolioAssetPreferenceService preferenceService) {
        this.preferenceService = preferenceService;
    }

    @GetMapping
    public ContributionEligibilityResponse getEligibility(@PathVariable Long portfolioId) {
        return preferenceService.getContributionEligibility(portfolioId);
    }

    @PutMapping
    public ContributionEligibilityResponse setEligibility(
            @PathVariable Long portfolioId,
            @RequestBody ContributionEligibilityRequest request) {
        return preferenceService.setContributionEligibility(portfolioId, request);
    }
}