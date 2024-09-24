package dev.jaoow.investmentapp.application.service.summary;

import dev.jaoow.investmentapp.application.dto.response.summary.AssetSummaryResponse;
import dev.jaoow.investmentapp.application.dto.response.summary.PortfolioSummaryResponse;
import dev.jaoow.investmentapp.application.service.consolidation.AssetConsolidationService;
import dev.jaoow.investmentapp.application.model.AssetConsolidation;
import dev.jaoow.investmentapp.domain.entity.Portfolio;
import dev.jaoow.investmentapp.domain.entity.Ticker;
import dev.jaoow.investmentapp.domain.repository.PortfolioRepository;
import dev.jaoow.investmentapp.domain.repository.TickerRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
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

    public PortfolioSummaryResponse getPortfolioSummary(Long portfolioId, String category) {
        log.info("Fetching portfolio summary for portfolio ID: {}", portfolioId);

        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> {
                    log.error("Portfolio not found with ID: {}", portfolioId);
                    return new RuntimeException("Portfolio not found");
                });

        List<AssetConsolidation> assetConsolidations = assetConsolidationService.processAssetMovements(portfolio);
        List<AssetConsolidation> filteredAssets = category != null
                ? filterByCategory(assetConsolidations, category)
                : assetConsolidations;

        List<AssetSummaryResponse> assetSummaries = assetSummaryService.createAssetSummaries(filteredAssets);

        BigDecimal totalInvested = filteredAssets.stream()
                .map(AssetConsolidation::getInvestedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal currentValue = assetSummaries.stream()
                .map(AssetSummaryResponse::getCurrentValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("Successfully calculated portfolio summary for portfolio ID: {}", portfolioId);

        return buildPortfolioSummary(portfolioId, totalInvested, currentValue, assetSummaries);
    }

    private List<AssetConsolidation> filterByCategory(List<AssetConsolidation> assetConsolidations, String category) {
        return assetConsolidations.stream().filter(assetConsolidation -> {
            String tickerSymbol = assetConsolidation.getTickerSymbol();
            Optional<Ticker> optionalTicker = tickerRepository.findById(tickerSymbol);
            if (optionalTicker.isPresent()) {
                Ticker ticker = optionalTicker.get();
                return ticker.getCategory().name().equalsIgnoreCase(category);
            } else {
                log.warn("Ticker not found for symbol: {}", tickerSymbol);
                return false;
            }
        }).toList();
    }

    private PortfolioSummaryResponse buildPortfolioSummary(Long portfolioId, BigDecimal totalInvested,
                                                           BigDecimal currentValue, List<AssetSummaryResponse> assetSummaries) {
        BigDecimal overallProfitOrLoss = currentValue.subtract(totalInvested);
        BigDecimal overallPercentageChange = calculatePercentageChange(totalInvested, overallProfitOrLoss);

        return PortfolioSummaryResponse.builder()
                .portfolioId(portfolioId)
                .totalInvested(totalInvested)
                .currentValue(currentValue)
                .profitOrLoss(overallProfitOrLoss)
                .percentageChange(overallPercentageChange)
                .assetSummaries(assetSummaries)
                .build();
    }

    private BigDecimal calculatePercentageChange(BigDecimal investedAmount, BigDecimal profitOrLoss) {
        return investedAmount.equals(BigDecimal.ZERO) ? BigDecimal.ZERO :
                profitOrLoss.divide(investedAmount, 2, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
    }
}
