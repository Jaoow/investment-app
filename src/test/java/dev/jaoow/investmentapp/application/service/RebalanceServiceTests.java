package dev.jaoow.investmentapp.application.service;

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
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
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
        when(quoteProvider.getQuote("ABC")).thenReturn(quote("ABC", "20", now));
        when(quoteProvider.getQuote("DEF")).thenReturn(quote("DEF", "40", now));

        RebalanceService service = new RebalanceService(
                portfolioRepository,
                allocationRepository,
                new AssetConsolidationService(),
                quoteProvider,
                freshness
        );

        var recommendations = service.generateRebalanceRecommendations(1L, null);

        assertEquals(1, recommendations.size());
        assertEquals("ABC", recommendations.getFirst().getTickerSymbol());
        assertEquals(new BigDecimal("1"), recommendations.getFirst().getQuantity());
        assertEquals(new BigDecimal("50.0000"), recommendations.getFirst().getCurrentPercentage());
        assertEquals(new BigDecimal("75.0000"), recommendations.getFirst().getTargetPercentage());
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
