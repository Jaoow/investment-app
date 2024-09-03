package dev.jaoow.investmentapp.application.service;

import dev.jaoow.investmentapp.application.dto.request.CategoryAllocationRequest;
import dev.jaoow.investmentapp.application.dto.response.CategoryAllocationResponse;
import dev.jaoow.investmentapp.application.dto.response.RebalanceRecommendationResponse;
import dev.jaoow.investmentapp.domain.entity.*;
import dev.jaoow.investmentapp.domain.model.*;
import dev.jaoow.investmentapp.domain.repository.CategoryAllocationRepository;
import dev.jaoow.investmentapp.domain.repository.PortfolioRepository;
import dev.jaoow.investmentapp.domain.repository.TickerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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

    // Method to set target allocations for a portfolio
    @Transactional
    public void setCategoryAllocations(Long portfolioId, List<CategoryAllocationRequest> categoryAllocations) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new RuntimeException("Portfolio not found"));

        // Clear existing category allocations for the portfolio
        categoryAllocationRepository.deleteAllByPortfolioId(portfolioId);

        // Save new category-level and asset-level allocations
        for (CategoryAllocationRequest categoryRequest : categoryAllocations) {
            CategoryAllocation categoryAllocation = new CategoryAllocation();
            categoryAllocation.setPortfolio(portfolio);
            categoryAllocation.setCategory(categoryRequest.getCategory());
            categoryAllocation.setTargetPercentage(categoryRequest.getCategoryTargetPercentage());

            // Convert asset allocation requests to entity objects
            List<AssetAllocation> assetAllocations = categoryRequest.getAssetAllocations().stream().map(assetRequest -> {
                AssetAllocation assetAllocation = new AssetAllocation();
                assetAllocation.setCategoryAllocation(categoryAllocation);
                assetAllocation.setTickerSymbol(assetRequest.getTickerSymbol());
                assetAllocation.setTargetPercentage(assetRequest.getTargetPercentage());
                return assetAllocation;
            }).collect(Collectors.toList());

            // Set the asset allocations for the category
            categoryAllocation.setAssetAllocations(assetAllocations);

            // Save the category allocation (and its associated asset allocations)
            categoryAllocationRepository.save(categoryAllocation);
        }
    }

    // Method to generate rebalancing recommendations, only suggesting purchases
    public List<RebalanceRecommendationResponse> generateRebalancingRecommendations(Long portfolioId, String categoryFilter) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new RuntimeException("Portfolio not found"));

        // Fetch current asset movements and calculate their current values in the portfolio
        Map<String, BigDecimal> currentValues = new HashMap<>();
        BigDecimal totalPortfolioValue = BigDecimal.ZERO;

        for (AssetMovement movement : portfolio.getAssetMovements()) {
            Ticker ticker = tickerRepository.findById(movement.getTickerSymbol()).orElse(null);

            // Apply category filter if provided
            if (ticker != null && (categoryFilter == null || ticker.getCategory().name().equalsIgnoreCase(categoryFilter))) {
                BigDecimal movementValue = movement.getQuantity().multiply(movement.getPrice());
                currentValues.put(movement.getTickerSymbol(), currentValues.getOrDefault(movement.getTickerSymbol(), BigDecimal.ZERO).add(movementValue));
                totalPortfolioValue = totalPortfolioValue.add(movementValue);
            }
        }

        // Fetch target category allocations and their asset allocations
        List<CategoryAllocation> categoryAllocations = categoryAllocationRepository.findByPortfolioId(portfolioId);

        List<RebalanceRecommendationResponse> recommendations = new ArrayList<>();

        for (CategoryAllocation categoryAllocation : categoryAllocations) {
            if (categoryFilter != null && !categoryAllocation.getCategory().name().equalsIgnoreCase(categoryFilter)) {
                continue;
            }

            BigDecimal currentCategoryValue = BigDecimal.ZERO;
            double totalCategoryTargetPercentage = categoryAllocation.getTargetPercentage();

            // Calculate current value of assets in the category
            for (AssetAllocation assetAllocation : categoryAllocation.getAssetAllocations()) {
                BigDecimal currentValue = currentValues.getOrDefault(assetAllocation.getTickerSymbol(), BigDecimal.ZERO);
                currentCategoryValue = currentCategoryValue.add(currentValue);
            }

            double currentCategoryPercentage = totalPortfolioValue.equals(BigDecimal.ZERO) ? 0.0 :
                    currentCategoryValue.divide(totalPortfolioValue, BigDecimal.ROUND_HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue();

            // Only suggest purchases to reach the target allocation
            if (currentCategoryPercentage < totalCategoryTargetPercentage) {
                BigDecimal targetCategoryValue = totalPortfolioValue.multiply(BigDecimal.valueOf(totalCategoryTargetPercentage / 100));
                BigDecimal amountToBuy = targetCategoryValue.subtract(currentCategoryValue);

                for (AssetAllocation assetAllocation : categoryAllocation.getAssetAllocations()) {
                    double assetTargetPercentage = assetAllocation.getTargetPercentage();
                    BigDecimal targetAssetValue = totalPortfolioValue.multiply(BigDecimal.valueOf(assetTargetPercentage / 100));
                    BigDecimal currentValue = currentValues.getOrDefault(assetAllocation.getTickerSymbol(), BigDecimal.ZERO);
                    BigDecimal amountToAllocate = targetAssetValue.subtract(currentValue);
                    BigDecimal quantityToBuy = amountToAllocate.divide(currentValue, BigDecimal.ROUND_HALF_UP);

                    if (amountToAllocate.compareTo(BigDecimal.ZERO) > 0) {
                        RebalanceRecommendationResponse recommendation = new RebalanceRecommendationResponse();
                        recommendation.setTickerSymbol(assetAllocation.getTickerSymbol());
                        recommendation.setAction("BUY");
                        recommendation.setQuantity(quantityToBuy);
                        recommendation.setCurrentPercentage(currentCategoryPercentage);
                        recommendation.setTargetPercentage(assetTargetPercentage);

                        recommendations.add(recommendation);
                    }
                }
            }
        }

        return recommendations;
    }

    // Method to generate a list with real data and zeroed allocations for non-configured assets
    public List<CategoryAllocationResponse> getAllocationsWithZeroValues(Long portfolioId) {
        portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new RuntimeException("Portfolio not found"));

        List<CategoryAllocation> existingAllocations = categoryAllocationRepository.findByPortfolioId(portfolioId);

        // Convert existing allocations to a response format
        List<CategoryAllocationResponse> categoryAllocationResponses = existingAllocations.stream().map(categoryAllocation -> {
            List<CategoryAllocationResponse.AssetAllocationResponse> assetResponses = categoryAllocation.getAssetAllocations().stream().map(assetAllocation -> {
                CategoryAllocationResponse.AssetAllocationResponse assetResponse = new CategoryAllocationResponse.AssetAllocationResponse();
                assetResponse.setTickerSymbol(assetAllocation.getTickerSymbol());
                assetResponse.setTargetPercentage(assetAllocation.getTargetPercentage());
                return assetResponse;
            }).collect(Collectors.toList());

            CategoryAllocationResponse response = new CategoryAllocationResponse();
            response.setCategory(categoryAllocation.getCategory());
            response.setCategoryTargetPercentage(categoryAllocation.getTargetPercentage());
            response.setAssetAllocations(assetResponses);

            return response;
        }).collect(Collectors.toList());

        // Add categories that do not have any allocations (mock data with zero values)
        for (AssetCategory category : AssetCategory.values()) {
            boolean categoryExists = existingAllocations.stream().anyMatch(existing -> existing.getCategory() == category);
            if (!categoryExists) {
                List<Ticker> tickers = tickerRepository.findAllByCategory(category);
                List<CategoryAllocationResponse.AssetAllocationResponse> assetResponses = tickers.stream().map(ticker -> {
                    CategoryAllocationResponse.AssetAllocationResponse assetResponse = new CategoryAllocationResponse.AssetAllocationResponse();
                    assetResponse.setTickerSymbol(ticker.getSymbol());
                    assetResponse.setTargetPercentage(0.0);
                    return assetResponse;
                }).collect(Collectors.toList());

                CategoryAllocationResponse response = new CategoryAllocationResponse();
                response.setCategory(category);
                response.setCategoryTargetPercentage(0.0);
                response.setAssetAllocations(assetResponses);

                categoryAllocationResponses.add(response);
            }
        }

        return categoryAllocationResponses;
    }
}
