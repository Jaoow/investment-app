package dev.jaoow.investmentapp.application.service;

import dev.jaoow.investmentapp.application.dto.request.CategoryAllocationRequest;
import dev.jaoow.investmentapp.application.dto.response.CategoryAllocationResponse;
import dev.jaoow.investmentapp.application.exception.InvalidAllocationException;
import dev.jaoow.investmentapp.application.exception.PortfolioNotFoundException;
import dev.jaoow.investmentapp.application.exception.TickerNotFoundException;
import dev.jaoow.investmentapp.domain.entity.*;
import dev.jaoow.investmentapp.domain.model.AssetCategory;
import dev.jaoow.investmentapp.domain.repository.CategoryAllocationRepository;
import dev.jaoow.investmentapp.domain.repository.PortfolioRepository;
import dev.jaoow.investmentapp.domain.repository.TickerRepository;
import dev.jaoow.investmentapp.application.util.TickerSymbol;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    @PreAuthorize("hasRole('USER') and @portfolioSecurity.isOwner(#portfolioId, authentication)")
    public void setCategoryAllocations(Long portfolioId, List<CategoryAllocationRequest> categoryAllocations) {
        validateAllocations(categoryAllocations);

        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new PortfolioNotFoundException(portfolioId));

        categoryAllocationRepository.deleteAllByPortfolioId(portfolioId);
        // Flush removals before inserting replacements with the same unique category key.
        categoryAllocationRepository.flush();

        for (CategoryAllocationRequest request : categoryAllocations) {
            CategoryAllocation categoryAllocation = createCategoryAllocation(request, portfolio);
            List<AssetAllocation> assetAllocations = mapAssetAllocations(request, categoryAllocation);

            categoryAllocation.setAssetAllocations(assetAllocations);
            categoryAllocationRepository.save(categoryAllocation);
        }
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('USER') and @portfolioSecurity.isOwner(#portfolioId, authentication)")
    public List<CategoryAllocationResponse> getAllocationsWithZeroValues(Long portfolioId) {
        List<CategoryAllocation> existingAllocations = categoryAllocationRepository.findByPortfolioId(portfolioId);
        List<CategoryAllocationResponse> allocationResponses = new ArrayList<>(mapAllocationsToResponse(existingAllocations));

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
        if (request.getAssetAllocations() == null) {
            return List.of();
        }
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
                CategoryAllocationResponse response = new CategoryAllocationResponse();
                response.setCategory(category);
                response.setCategoryTargetPercentage(BigDecimal.ZERO);
                response.setAssetAllocations(List.of());

                allocationResponses.add(response);
            }
        }

        return allocationResponses;
    }

    private void validateAllocations(List<CategoryAllocationRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new InvalidAllocationException("At least one category allocation is required.");
        }

        Set<AssetCategory> categories = new HashSet<>();
        BigDecimal categoryTotal = BigDecimal.ZERO;

        for (CategoryAllocationRequest request : requests) {
            if (!categories.add(request.getCategory())) {
                throw new InvalidAllocationException("Each asset category can only be configured once.");
            }

            categoryTotal = categoryTotal.add(request.getCategoryTargetPercentage());
            boolean active = request.getCategoryTargetPercentage().compareTo(BigDecimal.ZERO) > 0;

            BigDecimal assetTotal = BigDecimal.ZERO;
            Set<String> tickers = new HashSet<>();
            for (CategoryAllocationRequest.AssetAllocationRequest assetRequest :
                    request.getAssetAllocations() == null
                            ? List.<CategoryAllocationRequest.AssetAllocationRequest>of()
                            : request.getAssetAllocations()) {
                assetRequest.setTickerSymbol(TickerSymbol.normalize(assetRequest.getTickerSymbol()));
                if (!tickers.add(assetRequest.getTickerSymbol())) {
                    throw new InvalidAllocationException("Each ticker can only be configured once per category.");
                }

                Ticker ticker = tickerRepository.findById(assetRequest.getTickerSymbol())
                        .orElseThrow(() -> new TickerNotFoundException(assetRequest.getTickerSymbol()));
                if (ticker.getCategory() != request.getCategory()) {
                    throw new InvalidAllocationException("Ticker " + ticker.getSymbol()
                            + " does not belong to category " + request.getCategory() + ".");
                }
                assetTotal = assetTotal.add(assetRequest.getTargetPercentage());
            }

            if (active && !tickers.isEmpty() && assetTotal.compareTo(BigDecimal.valueOf(100)) != 0) {
                throw new InvalidAllocationException("Asset targets within a category must total 100%.");
            }
        }

        if (categoryTotal.compareTo(BigDecimal.valueOf(100)) != 0) {
            throw new InvalidAllocationException("Category targets must total 100%.");
        }
    }
}
