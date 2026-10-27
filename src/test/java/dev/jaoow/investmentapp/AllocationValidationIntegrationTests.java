package dev.jaoow.investmentapp;

import dev.jaoow.investmentapp.domain.entity.Portfolio;
import dev.jaoow.investmentapp.domain.entity.Ticker;
import dev.jaoow.investmentapp.domain.entity.user.User;
import dev.jaoow.investmentapp.domain.model.AssetCategory;
import dev.jaoow.investmentapp.domain.repository.PortfolioRepository;
import dev.jaoow.investmentapp.domain.repository.TickerRepository;
import dev.jaoow.investmentapp.domain.repository.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "allocation-owner@example.com", roles = "USER")
class AllocationValidationIntegrationTests {
    @Autowired private MockMvc mvc;
    @Autowired private UserRepository users;
    @Autowired private PortfolioRepository portfolios;
    @Autowired private TickerRepository tickers;
    private Long portfolioId;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setName("Allocation owner");
        user.setEmail("allocation-owner@example.com");
        user.setPassword("not-used");
        Portfolio portfolio = new Portfolio();
        portfolio.setName("Allocation validation");
        portfolio.setUser(users.save(user));
        portfolioId = portfolios.save(portfolio).getId();
        for (String symbol : new String[]{"ALLOC3", "ALLOC4"}) {
            Ticker ticker = new Ticker();
            ticker.setSymbol(symbol);
            ticker.setCategory(AssetCategory.EQUITIES);
            tickers.save(ticker);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT"})
    void acceptsPartialExactAndZeroTargets(String method) throws Exception {
        for (String target : new String[]{"60.00", "100.00", "0.00"}) {
            write(method, """
                    [{"category":"EQUITIES","categoryTargetPercentage":%s,
                      "assetAllocations":[{"tickerSymbol":"ALLOC3","targetPercentage":33.33},
                                          {"tickerSymbol":"ALLOC4","targetPercentage":66.67}]}]
                    """.formatted(target), method.equals("POST") ? 201 : 200);
        }
        write(method, """
                [{"category":"EQUITIES","categoryTargetPercentage":60,
                  "assetAllocations":[{"tickerSymbol":"ALLOC3","targetPercentage":25},
                                      {"tickerSymbol":"ALLOC4","targetPercentage":25}]},
                 {"category":"BDRS","categoryTargetPercentage":20}]
                """, method.equals("POST") ? 201 : 200);
        assertEquitiesTarget(60);
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT"})
    void rejectsCategoryTotalAbove100WithoutReplacingSavedTargets(String method) throws Exception {
        saveInitialTargets();
        write(method, """
                [{"category":"EQUITIES","categoryTargetPercentage":60},
                 {"category":"BDRS","categoryTargetPercentage":40.01}]
                """, 400);
        assertEquitiesTarget(50);
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT"})
    void rejectsAssetTotalAbove100EvenInZeroTargetCategory(String method) throws Exception {
        saveInitialTargets();
        for (String categoryTarget : new String[]{"0", "50"}) {
            write(method, """
                    [{"category":"EQUITIES","categoryTargetPercentage":%s,
                      "assetAllocations":[{"tickerSymbol":"ALLOC3","targetPercentage":60},
                                          {"tickerSymbol":"ALLOC4","targetPercentage":40.01}]}]
                    """.formatted(categoryTarget), 400);
            assertEquitiesTarget(50);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT"})
    void rejectsInvalidIndividualTargets(String method) throws Exception {
        saveInitialTargets();
        for (String value : new String[]{"-0.01", "100.01", "null"}) {
            mvc.perform(request(HttpMethod.valueOf(method),
                            "/v1/portfolio/{id}/allocations", portfolioId)
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            [{"category":"EQUITIES","categoryTargetPercentage":%s}]
                            """.formatted(value)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.details.violations").isNotEmpty());
            assertEquitiesTarget(50);
        }
        for (String body : new String[]{"[]", "[null]", """
                [{"category":"EQUITIES","categoryTargetPercentage":50,"assetAllocations":[null]}]
                """, """
                [{"category":"EQUITIES","categoryTargetPercentage":50,
                  "assetAllocations":[{"tickerSymbol":"ALLOC3","targetPercentage":null}]}]
                """}) {
            mvc.perform(request(HttpMethod.valueOf(method),
                            "/v1/portfolio/{id}/allocations", portfolioId)
                    .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
            assertEquitiesTarget(50);
        }
    }

    private void saveInitialTargets() throws Exception {
        write("PUT", """
                [{"category":"EQUITIES","categoryTargetPercentage":50,"assetAllocations":[]}]
                """, 200);
    }

    private void write(String method, String body, int expectedStatus) throws Exception {
        var result = mvc.perform(request(HttpMethod.valueOf(method),
                        "/v1/portfolio/{id}/allocations", portfolioId)
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().is(expectedStatus));
        if (expectedStatus == 400) {
            result.andExpect(jsonPath("$.code").value("INVALID_ALLOCATION"));
        }
    }

    private void assertEquitiesTarget(int target) throws Exception {
        mvc.perform(get("/v1/portfolio/{id}/allocations", portfolioId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.category == 'EQUITIES')].categoryTargetPercentage")
                        .value(contains(target)));
    }
}
