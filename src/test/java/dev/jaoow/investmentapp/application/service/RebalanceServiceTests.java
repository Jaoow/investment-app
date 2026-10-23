package dev.jaoow.investmentapp.application.service;

import dev.jaoow.investmentapp.application.dto.request.RebalanceSaleRequest;
import dev.jaoow.investmentapp.application.model.MarketQuote;
import dev.jaoow.investmentapp.application.service.consolidation.AssetConsolidationService;
import dev.jaoow.investmentapp.domain.entity.AssetAllocation;
import dev.jaoow.investmentapp.domain.entity.AssetMovement;
import dev.jaoow.investmentapp.domain.entity.CategoryAllocation;
import dev.jaoow.investmentapp.domain.entity.Portfolio;
import dev.jaoow.investmentapp.domain.entity.user.User;
import dev.jaoow.investmentapp.domain.model.AssetCategory;
import dev.jaoow.investmentapp.domain.model.MovementType;
import dev.jaoow.investmentapp.domain.repository.CategoryAllocationRepository;
import dev.jaoow.investmentapp.domain.repository.PortfolioRepository;
import dev.jaoow.investmentapp.domain.repository.TickerRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RebalanceServiceTests {

    @Test
    void usesNetUnitsCurrentQuotesAndCombinedCategoryAssetTargets() {
        PortfolioRepository portfolioRepository = mock(PortfolioRepository.class);
        CategoryAllocationRepository allocationRepository = mock(CategoryAllocationRepository.class);
        MarketQuoteProvider quoteProvider = mock(MarketQuoteProvider.class);
        Instant now = Instant.parse("2026-10-02T12:00:00Z");
        MarketQuoteFreshness freshness = new MarketQuoteFreshness(Clock.fixed(now, Clock.systemUTC().getZone()),
                Duration.ofMinutes(15));

        Portfolio portfolio = new Portfolio();
        portfolio.setId(1L);
        User owner = new User();
        owner.setEmail("owner@example.com");
        portfolio.setUser(owner);
        portfolio.setAssetMovements(List.of(
                movement("ABC", "3", "10", MovementType.BUY),
                movement("ABC", "1", "12", MovementType.SELL),
                movement("DEF", "1", "20", MovementType.BUY)
        ));
        when(portfolioRepository.findById(1L)).thenReturn(Optional.of(portfolio));

        CategoryAllocation category = new CategoryAllocation();
        category.setCategory(AssetCategory.EQUITIES);
        category.setTargetPercentage(new BigDecimal("100.00"));
        AssetAllocation abc = allocation(category, "ABC", "75.00");
        AssetAllocation def = allocation(category, "DEF", "25.00");
        category.setAssetAllocations(List.of(abc, def));
        when(allocationRepository.findByPortfolioId(1L)).thenReturn(List.of(category));
        when(quoteProvider.getQuotes(List.of("ABC", "DEF"))).thenReturn(Map.of(
                "ABC", quote("ABC", "20", now),
                "DEF", quote("DEF", "40", now)));

        RebalanceService service = new RebalanceService(
                portfolioRepository,
                allocationRepository,
                new AssetConsolidationService(),
                new MarketQuoteLoader(quoteProvider, freshness),
                mock(TickerRepository.class)
        );

        var recommendations = service.generateRebalanceRecommendations(1L, null);

        assertEquals(1, recommendations.size());
        assertEquals("ABC", recommendations.getFirst().getTickerSymbol());
        assertEquals(new BigDecimal("1"), recommendations.getFirst().getQuantity());
        assertEquals(new BigDecimal("50.0000"), recommendations.getFirst().getCurrentPercentage());
        assertEquals(new BigDecimal("75.0000"), recommendations.getFirst().getTargetPercentage());
        verify(quoteProvider, times(1)).getQuotes(List.of("ABC", "DEF"));
        verify(quoteProvider, never()).getQuote(anyString());
    }

    @Test
    void reallocationSellsWholeUnitsAboveTargetAndProjectsReinvestment() {
        RebalanceService service = salePlanService();

        var plan = service.planSales(1L, new RebalanceSaleRequest(RebalanceSaleRequest.Mode.REALLOCATION, null));

        assertEquals(1, plan.sales().size());
        var sale = plan.sales().getFirst();
        assertEquals("ABC", sale.ticker());
        assertEquals(new BigDecimal("2"), sale.quantity());
        assertEquals(new BigDecimal("40.00"), sale.estimatedValue());
        assertEquals(new BigDecimal("20.00"), sale.realizedProfit());
        assertEquals(new BigDecimal("66.6667"), sale.currentPercentage());
        assertEquals(new BigDecimal("50.0000"), sale.targetPercentage());
        assertEquals(new BigDecimal("53.3333"), sale.projectedPercentage());
        assertEquals(new BigDecimal("40.00"), plan.saleAmount());
        assertEquals(new BigDecimal("40.00"), plan.reinvestedAmount());
        assertEquals(false, plan.configurationRequired());
        assertEquals(2, plan.categoryImpact().size());
        var equities = plan.categoryImpact().getFirst();
        assertEquals(AssetCategory.EQUITIES, equities.category());
        assertEquals(new BigDecimal("66.6667"), equities.currentPercentage());
        assertEquals(new BigDecimal("53.3333"), equities.projectedPercentage());
        assertEquals(new BigDecimal("46.6667"), plan.categoryImpact().get(1).projectedPercentage());
    }

    @Test
    void profitTakingKeepsInvestedCapitalAndRespectsWithdrawalLimit() {
        RebalanceService service = salePlanService();

        var maxPlan = service.planSales(1L, new RebalanceSaleRequest(RebalanceSaleRequest.Mode.PROFIT_TAKING, null));
        var limitedPlan = service.planSales(1L,
                new RebalanceSaleRequest(RebalanceSaleRequest.Mode.PROFIT_TAKING, new BigDecimal("50")));

        assertEquals(new BigDecimal("5"), maxPlan.sales().getFirst().quantity());
        assertEquals(new BigDecimal("100.00"), maxPlan.saleAmount());
        assertEquals(new BigDecimal("100.00"), maxPlan.availableProfit());
        assertEquals(new BigDecimal("100.00"), maxPlan.preservedPrincipal());
        assertEquals(1, maxPlan.profitableAssets());
        assertEquals(new BigDecimal("100.00"), maxPlan.maxWithdrawalAmount());
        assertEquals("DEF", maxPlan.excludedAssets().getFirst().getTickerSymbol());
        assertEquals(new BigDecimal("2"), limitedPlan.sales().getFirst().quantity());
        assertEquals(new BigDecimal("40.00"), limitedPlan.saleAmount());
        assertEquals(new BigDecimal("100.00"), limitedPlan.maxWithdrawalAmount());
    }

    private RebalanceService salePlanService() {
        PortfolioRepository portfolioRepository = mock(PortfolioRepository.class);
        CategoryAllocationRepository allocationRepository = mock(CategoryAllocationRepository.class);
        MarketQuoteProvider quoteProvider = mock(MarketQuoteProvider.class);
        Instant now = Instant.parse("2026-10-02T12:00:00Z");
        MarketQuoteFreshness freshness = new MarketQuoteFreshness(Clock.fixed(now, Clock.systemUTC().getZone()),
                Duration.ofMinutes(15));

        Portfolio portfolio = new Portfolio();
        portfolio.setId(1L);
        portfolio.setAssetMovements(List.of(
                movement("ABC", "10", "10", MovementType.BUY),
                movement("DEF", "5", "20", MovementType.BUY)
        ));
        when(portfolioRepository.findById(1L)).thenReturn(Optional.of(portfolio));

        CategoryAllocation equities = new CategoryAllocation();
        equities.setCategory(AssetCategory.EQUITIES);
        equities.setTargetPercentage(new BigDecimal("50.00"));
        equities.setAssetAllocations(List.of(allocation(equities, "ABC", "100.00")));
        CategoryAllocation realEstate = new CategoryAllocation();
        realEstate.setCategory(AssetCategory.REAL_ESTATE_FUNDS);
        realEstate.setTargetPercentage(new BigDecimal("50.00"));
        realEstate.setAssetAllocations(List.of(allocation(realEstate, "DEF", "100.00")));
        when(allocationRepository.findByPortfolioId(1L)).thenReturn(List.of(equities, realEstate));
        when(quoteProvider.getQuotes(List.of("ABC", "DEF"))).thenReturn(Map.of(
                "ABC", quote("ABC", "20", now),
                "DEF", quote("DEF", "20", now)));

        return new RebalanceService(portfolioRepository, allocationRepository, new AssetConsolidationService(),
                new MarketQuoteLoader(quoteProvider, freshness), mock(TickerRepository.class));
    }

    private AssetMovement movement(String ticker, String quantity, String price, MovementType type) {
        AssetMovement movement = new AssetMovement();
        movement.setTickerSymbol(ticker);
        movement.setQuantity(new BigDecimal(quantity));
        movement.setPrice(new BigDecimal(price));
        movement.setType(type);
        return movement;
    }

    private AssetAllocation allocation(CategoryAllocation category, String ticker, String percentage) {
        AssetAllocation allocation = new AssetAllocation();
        allocation.setCategoryAllocation(category);
        allocation.setTickerSymbol(ticker);
        allocation.setTargetPercentage(new BigDecimal(percentage));
        return allocation;
    }

    private MarketQuote quote(String ticker, String price, Instant now) {
        return new MarketQuote(
                ticker,
            ticker,
                new BigDecimal(price),
                "BRL",
                BigDecimal.ZERO,
                now,
                now,
                "test-provider"
        );
    }
}
