package dev.jaoow.investmentapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.jaoow.investmentapp.domain.entity.AssetMovement;
import dev.jaoow.investmentapp.domain.entity.Portfolio;
import dev.jaoow.investmentapp.domain.entity.user.User;
import dev.jaoow.investmentapp.domain.model.MovementType;
import dev.jaoow.investmentapp.domain.repository.AssetMovementRepository;
import dev.jaoow.investmentapp.domain.repository.PortfolioRepository;
import dev.jaoow.investmentapp.domain.repository.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "history-owner@example.com", roles = "USER")
class MovementPaginationIntegrationTests {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private UserRepository users;
    @Autowired private PortfolioRepository portfolios;
    @Autowired private AssetMovementRepository movements;

    @Test
    void traversesMoreThan100MovementsWithoutDuplicatesWhenDatesMatch() throws Exception {
        User user = new User();
        user.setName("History owner");
        user.setEmail("history-owner@example.com");
        user.setPassword("not-used");
        Portfolio portfolio = new Portfolio();
        portfolio.setName("History");
        portfolio.setUser(users.save(user));
        portfolio = portfolios.save(portfolio);
        List<Long> expectedIds = new ArrayList<>();
        for (int i = 0; i < 105; i++) {
            AssetMovement movement = new AssetMovement();
            movement.setPortfolio(portfolio);
            movement.setTickerSymbol("TEST4");
            movement.setQuantity(BigDecimal.ONE);
            movement.setPrice(BigDecimal.TEN);
            movement.setDate(LocalDate.of(2026, 10, 27));
            movement.setType(MovementType.BUY);
            expectedIds.add(movements.save(movement).getId());
        }
        Collections.reverse(expectedIds);
        List<Long> actualIds = new ArrayList<>();
        for (int page = 0; page < 5; page++) {
            String json = mvc.perform(get("/v1/portfolio/{id}/movement", portfolio.getId())
                            .param("page", String.valueOf(page)).param("size", "25")
                            .param("sort", "date,desc", "id,desc"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.page.totalElements").value(105))
                    .andExpect(jsonPath("$.content.length()").value(page == 4 ? 5 : 25))
                    .andReturn().getResponse().getContentAsString();
            mapper.readTree(json).get("content").forEach(item -> actualIds.add(item.get("id").asLong()));
        }
        assertEquals(expectedIds, actualIds);
    }
}
