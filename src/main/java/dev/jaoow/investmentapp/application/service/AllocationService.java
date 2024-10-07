package dev.jaoow.investmentapp.application.service;

import dev.jaoow.investmentapp.application.dto.request.CategoryAllocationRequest;
import dev.jaoow.investmentapp.application.dto.response.CategoryAllocationResponse;
import dev.jaoow.investmentapp.application.exception.PortfolioNotFoundException;
import dev.jaoow.investmentapp.domain.entity.*;
import dev.jaoow.investmentapp.domain.model.AssetCategory;
import dev.jaoow.investmentapp.domain.repository.CategoryAllocationRepository;
import dev.jaoow.investmentapp.domain.repository.PortfolioRepository;
import dev.jaoow.investmentapp.domain.repository.TickerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AllocationService {

    private final PortfolioRepository portfolioRepository;
    private final CategoryAllocationRepository categoryAllocationRepository;
    private final TickerRepository tickerRepository;

    public AllocationService(PortfolioRepository portfolioRepository,
                             CategoryAllocationRepository categoryAllocationRepository,
                             TickerRepository tickerRepository) {
        this.portfolioRepository = portfolioRepository;
        this.categoryAllocationRepository = categoryAllocationRepository;
        this.tickerRepository = tickerRepository;
    }

    @Transactional
    public void setCategoryAllocations(Long portfolioId, List<CategoryAllocationRequest> categoryAllocations) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new PortfolioNotFoundException(portfolioId));

        categoryAllocationRepository.deleteAllByPortfolioId(portfolioId);

        for (CategoryAllocationRequest request : categoryAllocations) {
            CategoryAllocation categoryAllocation = createCategoryAllocation(request, portfolio);
            List<AssetAllocation> assetAllocations = mapAssetAllocations(request, categoryAllocation);

            categoryAllocation.setAssetAllocations(assetAllocations);
            categoryAllocationRepository.save(categoryAllocation);
        }
    }

    public List<CategoryAllocationResponse> getAllocationsWithZeroValues(Long portfolioId) {
        List<CategoryAllocation> existingAllocations = categoryAllocationRepository.findByPortfolioId(portfolioId);
        List<CategoryAllocationResponse> allocationResponses = mapAllocationsToResponse(existingAllocations);

        return addMissingCategoriesWithZeroValues(portfolioId, allocationResponses);
    }

    private CategoryAllocation createCategoryAllocation(CategoryAllocationRequest request, Portfolio portfolio) {
        CategoryAllocation categoryAllocation = new CategoryAllocation();
        categoryAllocation.setPortfolio(portfolio);
        categoryAllocation.setCategory(request.getCategory());
        categoryAllocation.setTargetPercentage(request.getCategoryTargetPercentage());
        return categoryAllocation;
    }

    private List<AssetAllocation> mapAssetAllocations(CategoryAllocationRequest request, CategoryAllocation categoryAllocation) {
        return request.getAssetAllocations().stream()
                .map(assetRequest -> {
                    AssetAllocation assetAllocation = new AssetAllocation();
                    assetAllocation.setCategoryAllocation(categoryAllocation);
                    assetAllocation.setTickerSymbol(assetRequest.getTickerSymbol());
                    assetAllocation.setTargetPercentage(assetRequest.getTargetPercentage());
                    return assetAllocation;
                })
                .toList();
    }

    private List<CategoryAllocationResponse> mapAllocationsToResponse(List<CategoryAllocation> allocations) {
        return allocations.stream().map(categoryAllocation -> {
            List<CategoryAllocationResponse.AssetAllocationResponse> assetResponses = categoryAllocation.getAssetAllocations().stream()
                    .map(assetAllocation -> {
                        CategoryAllocationResponse.AssetAllocationResponse response = new CategoryAllocationResponse.AssetAllocationResponse();
                        response.setTickerSymbol(assetAllocation.getTickerSymbol());
                        response.setTargetPercentage(assetAllocation.getTargetPercentage());
                        return response;
                    })
                    .toList();

            CategoryAllocationResponse response = new CategoryAllocationResponse();
            response.setCategory(categoryAllocation.getCategory());
            response.setCategoryTargetPercentage(categoryAllocation.getTargetPercentage());
            response.setAssetAllocations(assetResponses);

            return response;
        }).toList();
    }

    private List<CategoryAllocationResponse> addMissingCategoriesWithZeroValues(Long portfolioId, List<CategoryAllocationResponse> allocationResponses) {
        for (AssetCategory category : AssetCategory.values()) {
            boolean categoryExists = allocationResponses.stream().anyMatch(response -> response.getCategory() == category);
            if (!categoryExists) {
                List<Ticker> tickers = tickerRepository.findAllByCategory(category);
                List<CategoryAllocationResponse.AssetAllocationResponse> assetResponses = tickers.stream()
                        .map(ticker -> {
                            CategoryAllocationResponse.AssetAllocationResponse response = new CategoryAllocationResponse.AssetAllocationResponse();
                            response.setTickerSymbol(ticker.getSymbol());
                            response.setTargetPercentage(0.0);
                            return response;
                        })
                        .toList();

                CategoryAllocationResponse response = new CategoryAllocationResponse();
                response.setCategory(category);
                response.setCategoryTargetPercentage(0.0);
                response.setAssetAllocations(assetResponses);

                allocationResponses.add(response);
            }
        }

        return allocationResponses;
    }
}
