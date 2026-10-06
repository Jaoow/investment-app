package dev.jaoow.investmentapp.infrastructure.web;

import dev.jaoow.investmentapp.application.dto.response.PortfolioClassDetailResponse;
import dev.jaoow.investmentapp.application.dto.response.PortfolioClassResponse;
import dev.jaoow.investmentapp.application.dto.response.PortfolioAssetDetailResponse;
import dev.jaoow.investmentapp.application.dto.response.PortfolioOverviewResponse;
import dev.jaoow.investmentapp.application.service.PortfolioAnalyticsService;
import dev.jaoow.investmentapp.application.service.summary.PortfolioSummaryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/portfolio/{portfolioId}")
public class PortfolioAnalyticsController {

    private final PortfolioAnalyticsService portfolioAnalyticsService;
    private final PortfolioSummaryService portfolioSummaryService;

    public PortfolioAnalyticsController(PortfolioAnalyticsService portfolioAnalyticsService, PortfolioSummaryService portfolioSummaryService) {
        this.portfolioAnalyticsService = portfolioAnalyticsService;
        this.portfolioSummaryService = portfolioSummaryService;
    }

    @GetMapping("/overview")
    public PortfolioOverviewResponse getOverview(@PathVariable Long portfolioId) {
        var summary = portfolioSummaryService.getPortfolioSummary(portfolioId, null);
        return new PortfolioOverviewResponse(
                summary.getPortfolioId(),
                summary.getCurrentValue(),
                summary.getTotalInvested(),
                summary.getProfitOrLoss(),
                summary.getPercentageChange(),
                summary.getAssetSummaries().size(),
                summary.getAssetSummaries());
    }

    @GetMapping("/classes")
    public List<PortfolioClassResponse> getClasses(@PathVariable Long portfolioId) {
        return portfolioAnalyticsService.getClasses(portfolioId);
    }

    @GetMapping("/classes/{classId}")
    public PortfolioClassDetailResponse getClassDetail(@PathVariable Long portfolioId, @PathVariable String classId) {
        return portfolioAnalyticsService.getClassDetail(portfolioId, classId);
    }

    @GetMapping("/assets/{ticker}")
    public PortfolioAssetDetailResponse getAssetDetail(@PathVariable Long portfolioId, @PathVariable String ticker) {
        return portfolioAnalyticsService.getAssetDetail(portfolioId, ticker);
    }
}