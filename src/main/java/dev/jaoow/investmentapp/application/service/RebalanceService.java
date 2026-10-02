package dev.jaoow.investmentapp.application.service;

import dev.jaoow.investmentapp.application.dto.response.RebalanceRecommendationResponse;
import dev.jaoow.investmentapp.application.exception.PortfolioNotFoundException;
import dev.jaoow.investmentapp.application.model.AssetConsolidation;
import dev.jaoow.investmentapp.application.model.MarketQuote;
import dev.jaoow.investmentapp.application.service.consolidation.AssetConsolidationService;
import dev.jaoow.investmentapp.domain.entity.*;
import dev.jaoow.investmentapp.domain.repository.CategoryAllocationRepository;
import dev.jaoow.investmentapp.domain.repository.PortfolioRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RebalanceService {

    private final PortfolioRepository portfolioRepository;
    private final CategoryAllocationRepository categoryAllocationRepository;
    private final AssetConsolidationService assetConsolidationService;
    private final MarketQuoteProvider marketQuoteProvider;
    private final MarketQuoteFreshness marketQuoteFreshness;

    public RebalanceService(PortfolioRepository portfolioRepository,
                            CategoryAllocationRepository categoryAllocationRepository,
                            AssetConsolidationService assetConsolidationService,
                            MarketQuoteProvider marketQuoteProvider,
                            MarketQuoteFreshness marketQuoteFreshness) {
        this.portfolioRepository = portfolioRepository;
        this.categoryAllocationRepository = categoryAllocationRepository;
        this.assetConsolidationService = assetConsolidationService;
        this.marketQuoteProvider = marketQuoteProvider;
        this.marketQuoteFreshness = marketQuoteFreshness;
    }

    @PreAuthorize("hasRole('USER') and @portfolioSecurity.isOwner(#portfolioId, authentication)")
    @Transactional(readOnly = true)
    public List<RebalanceRecommendationResponse> generateRebalanceRecommendations(Long portfolioId, String categoryFilter) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new PortfolioNotFoundException(portfolioId));
        List<CategoryAllocation> categoryAllocations = categoryAllocationRepository.findByPortfolioId(portfolioId);
        Map<String, BigDecimal> quantities = assetConsolidationService.processAssetMovements(portfolio).stream()
                .collect(Collectors.toMap(AssetConsolidation::getTickerSymbol, AssetConsolidation::getTotalQuantity));

        Set<String> tickers = new HashSet<>(categoryAllocations.stream()
                .flatMap(category -> category.getAssetAllocations().stream())
                .map(AssetAllocation::getTickerSymbol)
                .collect(Collectors.toSet()));
        tickers.addAll(quantities.keySet());

        Map<String, BigDecimal> prices = new HashMap<>();
        for (String ticker : tickers) {
            MarketQuote quote = marketQuoteFreshness.requireFresh(marketQuoteProvider.getQuote(ticker));
            prices.put(ticker, quote.price());
        }

        BigDecimal portfolioValue = quantities.entrySet().stream()
                .map(entry -> entry.getValue().multiply(prices.get(entry.getKey())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (portfolioValue.compareTo(BigDecimal.ZERO) == 0) {
            return List.of();
        }

        return categoryAllocations.stream()
                .filter(allocation -> categoryFilter == null
                        || allocation.getCategory().name().equalsIgnoreCase(categoryFilter))
                .flatMap(allocation -> allocation.getAssetAllocations().stream()
                        .map(assetAllocation -> buildRecommendation(allocation, assetAllocation, quantities, prices, portfolioValue)))
                .filter(recommendation -> recommendation != null)
                .toList();
    }

    private RebalanceRecommendationResponse buildRecommendation(
            CategoryAllocation categoryAllocation,
            AssetAllocation assetAllocation,
            Map<String, BigDecimal> quantities,
            Map<String, BigDecimal> prices,
            BigDecimal portfolioValue) {
        String ticker = assetAllocation.getTickerSymbol();
        BigDecimal price = prices.get(ticker);
        BigDecimal currentValue = quantities.getOrDefault(ticker, BigDecimal.ZERO).multiply(price);
        BigDecimal targetPercentage = categoryAllocation.getTargetPercentage()
                .multiply(assetAllocation.getTargetPercentage())
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal targetValue = portfolioValue.multiply(targetPercentage)
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal gap = targetValue.subtract(currentValue);
        if (gap.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        BigDecimal quantityToBuy = gap.divideToIntegralValue(price).setScale(0, RoundingMode.DOWN);
        if (quantityToBuy.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        RebalanceRecommendationResponse response = new RebalanceRecommendationResponse();
        response.setTickerSymbol(ticker);
        response.setAction("BUY");
        response.setQuantity(quantityToBuy);
        response.setCurrentPercentage(currentValue.multiply(BigDecimal.valueOf(100))
                .divide(portfolioValue, 4, RoundingMode.HALF_UP));
        response.setTargetPercentage(targetPercentage);
        return response;
    }
}
