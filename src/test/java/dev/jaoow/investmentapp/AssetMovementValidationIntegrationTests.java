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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "movement-owner@example.com", roles = "USER")
class AssetMovementValidationIntegrationTests {
    @Autowired private MockMvc mvc;
    @Autowired private UserRepository users;
    @Autowired private PortfolioRepository portfolios;
    private Long portfolioId;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setName("Movement owner");
        user.setEmail("movement-owner@example.com");
        user.setPassword("not-used");
        user = users.save(user);
        Portfolio portfolio = new Portfolio();
        portfolio.setName("Movements");
        portfolio.setUser(user);
        portfolioId = portfolios.save(portfolio).getId();
    }

    @Test
    void rejectsMovementForTickerOutsideCatalog() throws Exception {
        movement("FAKE99", "BUY", "1").andExpect(status().isBadRequest());
    }

    @Test
    void normalizesTickerAndRejectsSaleAboveHeldQuantity() throws Exception {
        mvc.perform(post("/v1/ticker/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"symbol\":\"TEST4\",\"category\":\"EQUITIES\"}"))
                .andExpect(status().isOk());
        movement(" test4 ", "BUY", "10").andExpect(status().isCreated())
                .andExpect(jsonPath("$.tickerSymbol").value("TEST4"));
        movement("TEST4", "SELL", "11").andExpect(status().isUnprocessableEntity());
        movement("TEST4", "SELL", "10").andExpect(status().isCreated());
        movement("TEST4", "SELL", "1").andExpect(status().isUnprocessableEntity());
    }

    private ResultActions movement(String symbol, String type, String quantity) throws Exception {
        return mvc.perform(post("/v1/portfolio/{id}/movement", portfolioId).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"tickerSymbol":"%s","quantity":%s,"price":10,"type":"%s","date":"2026-01-02"}
                        """.formatted(symbol, quantity, type)));
    }
}
