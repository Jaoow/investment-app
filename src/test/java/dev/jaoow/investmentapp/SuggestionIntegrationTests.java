package dev.jaoow.investmentapp;

import dev.jaoow.investmentapp.application.model.MarketQuote;
import dev.jaoow.investmentapp.domain.entity.AssetAllocation;
import dev.jaoow.investmentapp.domain.entity.CategoryAllocation;
import dev.jaoow.investmentapp.domain.entity.Portfolio;
import dev.jaoow.investmentapp.domain.entity.PortfolioAssetPreference;
import dev.jaoow.investmentapp.domain.entity.Ticker;
import dev.jaoow.investmentapp.domain.entity.user.User;
import dev.jaoow.investmentapp.domain.model.AssetCategory;
import dev.jaoow.investmentapp.domain.repository.CategoryAllocationRepository;
import dev.jaoow.investmentapp.domain.repository.PortfolioAssetPreferenceRepository;
import dev.jaoow.investmentapp.domain.repository.PortfolioRepository;
import dev.jaoow.investmentapp.domain.repository.TickerRepository;
import dev.jaoow.investmentapp.domain.repository.user.UserRepository;
import dev.jaoow.investmentapp.application.service.MarketQuoteProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SuggestionIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private TickerRepository tickerRepository;

    @Autowired
    private CategoryAllocationRepository categoryAllocationRepository;

    @Autowired
    private PortfolioAssetPreferenceRepository preferenceRepository;

    @MockBean
    private MarketQuoteProvider marketQuoteProvider;

    private Long portfolioId;

    @BeforeEach
    void setUp() {
        preferenceRepository.deleteAll();
        categoryAllocationRepository.deleteAll();
        portfolioRepository.deleteAll();
        tickerRepository.deleteAll();
        userRepository.deleteAll();

        User owner = new User();
        owner.setName("Owner");
        owner.setEmail("suggestion-owner@example.com");
        owner.setPassword("not-used-in-this-test");
        owner = userRepository.save(owner);

        Portfolio portfolio = new Portfolio();
        portfolio.setName("Suggestion portfolio");
        portfolio.setUser(owner);
        portfolio = portfolioRepository.save(portfolio);
        portfolioId = portfolio.getId();

        Ticker ticker = new Ticker();
        ticker.setSymbol("ABC");
        ticker.setCategory(AssetCategory.EQUITIES);
        tickerRepository.save(ticker);

        CategoryAllocation category = new CategoryAllocation();
        category.setPortfolio(portfolio);
        category.setCategory(AssetCategory.EQUITIES);
        category.setTargetPercentage(new BigDecimal("100.00"));

        AssetAllocation asset = new AssetAllocation();
        asset.setTickerSymbol("ABC");
        asset.setTargetPercentage(new BigDecimal("100.00"));
        asset.setCategoryAllocation(category);
        category.setAssetAllocations(List.of(asset));
        categoryAllocationRepository.save(category);

        PortfolioAssetPreference preference = new PortfolioAssetPreference();
        preference.setPortfolio(portfolio);
        preference.setTickerSymbol("ABC");
        preference.setPriceCeiling(new BigDecimal("100.00"));
        preferenceRepository.save(preference);

        Instant now = Instant.now();
        given(marketQuoteProvider.getQuote("ABC")).willReturn(new MarketQuote(
                "ABC",
                new BigDecimal("88.00"),
                "BRL",
                new BigDecimal("-3.00"),
                now,
                now,
                "test-provider"
        ));
    }

    @Test
    @WithMockUser(username = "suggestion-owner@example.com", roles = "USER")
    void returnsWholeUnitSuggestionWithExplanationAndResidual() throws Exception {
        mockMvc.perform(post("/v1/portfolio/{id}/suggestions", portfolioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":150.00,\"currency\":\"BRL\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestedAmount").value(150))
                .andExpect(jsonPath("$.allocatedAmount").value(88))
                .andExpect(jsonPath("$.remainingAmount").value(62))
                .andExpect(jsonPath("$.items[0].tickerSymbol").value("ABC"))
                .andExpect(jsonPath("$.items[0].quantity").value(1))
                .andExpect(jsonPath("$.items[0].ceilingDistancePercentage").value(12))
                .andExpect(jsonPath("$.items[0].score").value(0.56))
                .andExpect(jsonPath("$.items[0].reasons").isArray())
                .andExpect(jsonPath("$.disclaimer").value(org.hamcrest.Matchers.containsString("não constitui consultoria")));
    }

    @Test
    @WithMockUser(username = "intruder@example.com", roles = "USER")
    void rejectsSuggestionsForAnotherUsersPortfolio() throws Exception {
        mockMvc.perform(post("/v1/portfolio/{id}/suggestions", portfolioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":150.00,\"currency\":\"BRL\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "suggestion-owner@example.com", roles = "USER")
    void blocksSuggestionsWhenMarketQuoteIsStale() throws Exception {
        Instant old = Instant.now().minusSeconds(3600);
        given(marketQuoteProvider.getQuote("ABC")).willReturn(new MarketQuote(
                "ABC",
                new BigDecimal("88.00"),
                "BRL",
                new BigDecimal("-3.00"),
                old,
                old,
                "test-provider"
        ));

        mockMvc.perform(post("/v1/portfolio/{id}/suggestions", portfolioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":150.00,\"currency\":\"BRL\"}"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    @WithMockUser(username = "suggestion-owner@example.com", roles = "USER")
    void storesCeilingPerPortfolioAndValidatesPositivePrice() throws Exception {
        mockMvc.perform(put("/v1/portfolio/{id}/assets/ABC/ceiling", portfolioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"priceCeiling\":120.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tickerSymbol").value("ABC"))
                .andExpect(jsonPath("$.priceCeiling").value(120));

        mockMvc.perform(put("/v1/portfolio/{id}/assets/ABC/ceiling", portfolioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"priceCeiling\":0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "suggestion-owner@example.com", roles = "USER")
    void rejectsInconsistentCategoryTargetsWithoutDeletingSavedAllocation() throws Exception {
        mockMvc.perform(post("/v1/portfolio/{id}/allocations", portfolioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [{
                                  "category":"EQUITIES",
                                  "categoryTargetPercentage":50.00,
                                  "assetAllocations":[{"tickerSymbol":"ABC","targetPercentage":100.00}]
                                }]
                                """))
                .andExpect(status().isBadRequest());

        assertEquals(1, categoryAllocationRepository.findByPortfolioId(portfolioId).size());
        assertEquals(0, categoryAllocationRepository.findByPortfolioId(portfolioId).getFirst()
                .getTargetPercentage().compareTo(new BigDecimal("100.00")));
    }
}
