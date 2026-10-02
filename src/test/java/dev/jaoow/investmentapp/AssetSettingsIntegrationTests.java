package dev.jaoow.investmentapp;

import dev.jaoow.investmentapp.domain.entity.Portfolio;
import dev.jaoow.investmentapp.domain.entity.user.User;
import dev.jaoow.investmentapp.domain.repository.PortfolioRepository;
import dev.jaoow.investmentapp.domain.repository.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "settings-owner@example.com", roles = "USER")
class AssetSettingsIntegrationTests {
    @Autowired private MockMvc mvc;
    @Autowired private UserRepository users;
    @Autowired private PortfolioRepository portfolios;
    private Long portfolioId;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setName("Settings owner");
        user.setEmail("settings-owner@example.com");
        user.setPassword("not-used");
        user = users.save(user);
        Portfolio portfolio = new Portfolio();
        portfolio.setName("Settings");
        portfolio.setUser(user);
        portfolioId = portfolios.save(portfolio).getId();
    }

    @Test
    void userCanRegisterTickerWithoutGlobalCeilingOrInventedSector() throws Exception {
        register(" test4 ", "EQUITIES");
        mvc.perform(post("/v1/ticker/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"symbol\":\"TEST4\",\"category\":\"EQUITIES\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.symbol").value("TEST4"));
        mvc.perform(post("/v1/ticker/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"symbol\":\"TEST4\",\"category\":\"BDRS\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/v1/ticker/TEST4"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.category").value("EQUITIES"));
    }

    @Test
    void recordsOnlyChangesAndPreservesHistoryAfterRemovalAndRecreation() throws Exception {
        register("TEST4", "EQUITIES");
        ceiling("test4", "32.5000");
        ceiling("TEST4", "35.2500");
        ceiling("TEST4", "35.25");
        mvc.perform(delete("/v1/portfolio/{id}/assets/TEST4/ceiling", portfolioId))
                .andExpect(status().isNoContent());
        ceiling("TEST4", "34.00");
        mvc.perform(get("/v1/portfolio/{id}/assets/TEST4/ceiling/history", portfolioId)
                        .param("size", "2").param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(4))
                .andExpect(jsonPath("$.content[0].priceCeiling").value(34))
                .andExpect(jsonPath("$.content[0].changedAt").isNotEmpty())
                .andExpect(jsonPath("$.content[1].action").value("REMOVED"))
                .andExpect(jsonPath("$.content[1].previousPrice").value(35.25));
        mvc.perform(get("/v1/portfolio/{id}/assets/TEST4/ceiling/history", portfolioId)
                        .param("size", "2").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].previousPrice").value(32.5))
                .andExpect(jsonPath("$.content[1].priceCeiling").value(32.5));
    }

    @Test
    void replacingTargetsWorksRepeatedlyAndSettingsShowEffectiveTargetWithoutMarketData() throws Exception {
        register("TEST4", "EQUITIES");
        register("TEST3", "EQUITIES");
        register("TEST11", "REAL_ESTATE_FUNDS");
        ceiling("TEST4", "32.50");
        allocations("60.00", "40.00", "25.00", "75.00");
        allocations("62.50", "37.50", "20.00", "80.00");
        mvc.perform(get("/v1/portfolio/{id}/allocations", portfolioId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.category == 'EQUITIES')].categoryTargetPercentage")
                        .value(org.hamcrest.Matchers.contains(62.5)));
        mvc.perform(get("/v1/portfolio/{id}/asset-settings", portfolioId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.tickerSymbol == 'TEST4')].allocationPercentage")
                        .value(org.hamcrest.Matchers.contains(12.5)))
                .andExpect(jsonPath("$[?(@.tickerSymbol == 'TEST4')].priceCeiling")
                        .value(org.hamcrest.Matchers.contains(32.5)));
        mvc.perform(post("/v1/portfolio/{id}/allocations", portfolioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"category\":\"BDRS\",\"categoryTargetPercentage\":100,"
                                + "\"assetAllocations\":[{\"tickerSymbol\":\"TEST4\",\"targetPercentage\":100}]}]"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/v1/portfolio/{id}/asset-settings", portfolioId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.tickerSymbol == 'TEST4')].allocationPercentage")
                        .value(org.hamcrest.Matchers.contains(12.5)));
    }

    @Test
    void historyAndSettingsArePrivateToPortfolioOwner() throws Exception {
        User other = new User();
        other.setName("Other");
        other.setEmail("other-settings@example.com");
        other.setPassword("not-used");
        other = users.save(other);
        Portfolio portfolio = new Portfolio();
        portfolio.setName("Other settings");
        portfolio.setUser(other);
        Long otherId = portfolios.save(portfolio).getId();
        mvc.perform(get("/v1/portfolio/{id}/asset-settings", otherId)).andExpect(status().isForbidden());
        mvc.perform(get("/v1/portfolio/{id}/assets/TEST4/ceiling/history", otherId)).andExpect(status().isForbidden());
        mvc.perform(put("/v1/portfolio/{id}/assets/TEST4/ceiling", otherId)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"priceCeiling\":10}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void emptyPortfolioDoesNotIncludeEveryTickerFromGlobalCatalog() throws Exception {
        register("TEST4", "EQUITIES");
        mvc.perform(get("/v1/portfolio/{id}/allocations", portfolioId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].assetAllocations").isEmpty());
        mvc.perform(get("/v1/portfolio/{id}/asset-settings", portfolioId))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    private void register(String symbol, String category) throws Exception {
        mvc.perform(post("/v1/ticker/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"symbol\":\"" + symbol + "\",\"category\":\"" + category + "\"}"))
                .andExpect(status().isOk());
    }

    private void ceiling(String symbol, String price) throws Exception {
        mvc.perform(put("/v1/portfolio/{id}/assets/{symbol}/ceiling", portfolioId, symbol)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"priceCeiling\":\"" + price + "\"}"))
                .andExpect(status().isOk());
    }

    private void allocations(String equities, String funds, String first, String second) throws Exception {
        mvc.perform(post("/v1/portfolio/{id}/allocations", portfolioId).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [{"category":"EQUITIES","categoryTargetPercentage":"%s",
                                  "assetAllocations":[{"tickerSymbol":"TEST4","targetPercentage":"%s"},
                                                      {"tickerSymbol":"TEST3","targetPercentage":"%s"}]},
                                 {"category":"REAL_ESTATE_FUNDS","categoryTargetPercentage":"%s",
                                  "assetAllocations":[{"tickerSymbol":"TEST11","targetPercentage":"100.00"}]}]
                                """.formatted(equities, first, second, funds)))
                .andExpect(status().isCreated());
    }
}
