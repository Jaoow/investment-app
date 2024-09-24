package dev.jaoow.investmentapp.application.service;

import dev.jaoow.investmentapp.application.dto.response.RebalanceRecommendationResponse;
import dev.jaoow.investmentapp.domain.entity.*;
import dev.jaoow.investmentapp.domain.repository.CategoryAllocationRepository;
import dev.jaoow.investmentapp.domain.repository.PortfolioRepository;
import dev.jaoow.investmentapp.domain.repository.TickerRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RebalanceService {

    private final PortfolioRepository portfolioRepository;
    private final CategoryAllocationRepository categoryAllocationRepository;
    private final TickerRepository tickerRepository;

    public RebalanceService(PortfolioRepository portfolioRepository,
                            CategoryAllocationRepository categoryAllocationRepository,
                            TickerRepository tickerRepository) {
        this.portfolioRepository = portfolioRepository;
        this.categoryAllocationRepository = categoryAllocationRepository;
        this.tickerRepository = tickerRepository;
    }

    public List<RebalanceRecommendationResponse> generateRebalanceRecommendations(Long portfolioId, String categoryFilter) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new RuntimeException("Portfolio not found"));
        BigDecimal totalPortfolioValue = calculateTotalPortfolioValue(portfolio, categoryFilter);

        List<CategoryAllocation> categoryAllocations = categoryAllocationRepository.findByPortfolioId(portfolioId);

        return categoryAllocations.stream()
            .filter(categoryAllocation -> filterByCategory(categoryFilter, categoryAllocation))
            .flatMap(categoryAllocation -> generateCategoryRebalanceRecommendations(categoryAllocation, portfolio, totalPortfolioValue).stream())
            .toList();
    }


    private BigDecimal calculateTotalPortfolioValue(Portfolio portfolio, String categoryFilter) {
        BigDecimal totalValue = BigDecimal.ZERO;
        for (AssetMovement movement : portfolio.getAssetMovements()) {
            Ticker ticker = tickerRepository.findById(movement.getTickerSymbol()).orElse(null);
            if (ticker != null && (categoryFilter == null || ticker.getCategory().name().equalsIgnoreCase(categoryFilter))) {
                BigDecimal movementValue = movement.getQuantity().multiply(movement.getPrice());
                totalValue = totalValue.add(movementValue);
            }
        }
        return totalValue;
    }

    private boolean filterByCategory(String categoryFilter, CategoryAllocation categoryAllocation) {
        return categoryFilter == null || categoryAllocation.getCategory().name().equalsIgnoreCase(categoryFilter);
    }

    private List<RebalanceRecommendationResponse> generateCategoryRebalanceRecommendations(
            CategoryAllocation categoryAllocation, Portfolio portfolio, BigDecimal totalPortfolioValue) {
        BigDecimal currentCategoryValue = calculateCurrentCategoryValue(categoryAllocation, portfolio);
        double currentCategoryPercentage = calculatePercentage(currentCategoryValue, totalPortfolioValue);
        double targetCategoryPercentage = categoryAllocation.getTargetPercentage();

        if (currentCategoryPercentage < targetCategoryPercentage) {
            return calculateAssetRecommendations(categoryAllocation, totalPortfolioValue, currentCategoryValue);
        }
        return Collections.emptyList();
    }

    private BigDecimal calculateCurrentCategoryValue(CategoryAllocation categoryAllocation, Portfolio portfolio) {
        return categoryAllocation.getAssetAllocations().stream()
                .map(assetAllocation -> portfolio.getAssetMovements().stream()
                        .filter(movement -> movement.getTickerSymbol().equals(assetAllocation.getTickerSymbol()))
                        .map(movement -> movement.getQuantity().multiply(movement.getPrice()))
                        .reduce(BigDecimal.ZERO, BigDecimal::add))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private double calculatePercentage(BigDecimal value, BigDecimal totalValue) {
        return totalValue.equals(BigDecimal.ZERO) ? 0.0 :
                value.divide(totalValue, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue();
    }

    private List<RebalanceRecommendationResponse> calculateAssetRecommendations(CategoryAllocation categoryAllocation, BigDecimal totalPortfolioValue, BigDecimal currentCategoryValue) {
        List<RebalanceRecommendationResponse> recommendations = new ArrayList<>();

        for (AssetAllocation assetAllocation : categoryAllocation.getAssetAllocations()) {
            BigDecimal targetAssetValue = totalPortfolioValue.multiply(BigDecimal.valueOf(assetAllocation.getTargetPercentage() / 100));
            BigDecimal amountToAllocate = targetAssetValue.subtract(currentCategoryValue);
            BigDecimal quantityToBuy = amountToAllocate.divide(currentCategoryValue, RoundingMode.HALF_UP);

            if (amountToAllocate.compareTo(BigDecimal.ZERO) > 0) {
                RebalanceRecommendationResponse recommendation = new RebalanceRecommendationResponse();
                recommendation.setTickerSymbol(assetAllocation.getTickerSymbol());
                recommendation.setAction("BUY");
                recommendation.setQuantity(quantityToBuy);
                recommendation.setCurrentPercentage(currentCategoryValue.doubleValue());
                recommendation.setTargetPercentage(assetAllocation.getTargetPercentage());
                recommendations.add(recommendation);
            }
        }

        return recommendations;
    }
}
