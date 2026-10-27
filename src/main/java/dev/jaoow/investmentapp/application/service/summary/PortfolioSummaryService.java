package dev.jaoow.investmentapp.application.service.summary;

import dev.jaoow.investmentapp.application.dto.response.summary.AssetSummaryResponse;
import dev.jaoow.investmentapp.application.dto.response.summary.PortfolioSummaryResponse;
import dev.jaoow.investmentapp.application.exception.PortfolioNotFoundException;
import dev.jaoow.investmentapp.application.service.consolidation.AssetConsolidationService;
import dev.jaoow.investmentapp.application.model.AssetConsolidation;
import dev.jaoow.investmentapp.domain.entity.Portfolio;
import dev.jaoow.investmentapp.domain.entity.Ticker;
import dev.jaoow.investmentapp.domain.repository.PortfolioRepository;
import dev.jaoow.investmentapp.domain.repository.TickerRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PortfolioSummaryService {

    private final PortfolioRepository portfolioRepository;
    private final AssetSummaryService assetSummaryService;
    private final TickerRepository tickerRepository;
    private final AssetConsolidationService assetConsolidationService;

    public PortfolioSummaryService(PortfolioRepository portfolioRepository, AssetSummaryService assetSummaryService,
                                   TickerRepository tickerRepository, AssetConsolidationService assetConsolidationService) {
        this.portfolioRepository = portfolioRepository;
        this.assetSummaryService = assetSummaryService;
        this.tickerRepository = tickerRepository;
        this.assetConsolidationService = assetConsolidationService;
    }

    @PreAuthorize("hasRole('USER') and @portfolioSecurity.isOwner(#portfolioId, authentication)")
    @Transactional
    public PortfolioSummaryResponse getPortfolioSummary(Long portfolioId, String category) {
        return getPortfolioSummary(portfolioId, category, true);
    }

    @PreAuthorize("hasRole('USER') and @portfolioSecurity.isOwner(#portfolioId, authentication)")
    @Transactional
    public PortfolioSummaryResponse getPortfolioSummary(Long portfolioId, String category, boolean includeQuotes) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new PortfolioNotFoundException(portfolioId));

        List<AssetConsolidation> assetConsolidations = assetConsolidationService.processAssetMovements(portfolio);
        List<AssetConsolidation> filteredAssets = category != null
                ? filterByCategory(assetConsolidations, category)
                : assetConsolidations;

        List<AssetSummaryResponse> assetSummaries = includeQuotes
                ? assetSummaryService.createAssetSummaries(filteredAssets)
                : assetSummaryService.createAssetSummariesWithoutQuotes(filteredAssets);

        BigDecimal totalInvested = filteredAssets.stream()
                .map(AssetConsolidation::getInvestedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal currentValue = assetSummaries.stream()
                .map(AssetSummaryResponse::getCurrentValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return buildPortfolioSummary(portfolioId, totalInvested, currentValue, assetSummaries);
    }

    private List<AssetConsolidation> filterByCategory(List<AssetConsolidation> assetConsolidations, String category) {
        Set<String> matchingSymbols = tickerRepository.findAllById(assetConsolidations.stream()
                        .map(AssetConsolidation::getTickerSymbol)
                        .toList()).stream()
                .filter(ticker -> ticker.getCategory() != null && ticker.getCategory().name().equalsIgnoreCase(category))
                .map(Ticker::getSymbol)
                .collect(Collectors.toSet());
        return assetConsolidations.stream()
                .filter(assetConsolidation -> matchingSymbols.contains(assetConsolidation.getTickerSymbol()))
                .toList();
    }

    private PortfolioSummaryResponse buildPortfolioSummary(Long portfolioId, BigDecimal totalInvested,
                                                           BigDecimal currentValue, List<AssetSummaryResponse> assetSummaries) {
        BigDecimal overallProfitOrLoss = currentValue.subtract(totalInvested);
        BigDecimal overallPercentageChange = SummaryMath.percentageChange(totalInvested, overallProfitOrLoss);

        return PortfolioSummaryResponse.builder()
                .portfolioId(portfolioId)
                .totalInvested(totalInvested)
                .currentValue(currentValue)
                .profitOrLoss(overallProfitOrLoss)
                .percentageChange(overallPercentageChange)
                .assetSummaries(assetSummaries)
                .build();
    }

}
