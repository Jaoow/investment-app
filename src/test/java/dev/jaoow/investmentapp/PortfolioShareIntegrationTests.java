package dev.jaoow.investmentapp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.jaoow.investmentapp.domain.entity.AssetMovement;
import dev.jaoow.investmentapp.domain.entity.Portfolio;
import dev.jaoow.investmentapp.domain.entity.PortfolioShare;
import dev.jaoow.investmentapp.domain.entity.user.User;
import dev.jaoow.investmentapp.domain.model.MovementType;
import dev.jaoow.investmentapp.domain.repository.PortfolioRepository;
import dev.jaoow.investmentapp.domain.repository.PortfolioShareRepository;
import dev.jaoow.investmentapp.domain.repository.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PortfolioShareIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private PortfolioShareRepository shareRepository;

    private Long portfolioId;

    @BeforeEach
    void setUp() {
        shareRepository.deleteAll();
        portfolioRepository.deleteAll();
        userRepository.deleteAll();

        User owner = new User();
        owner.setName("Share Owner");
        owner.setEmail("share-owner@example.com");
        owner.setPassword("not-used-in-this-test");
        owner = userRepository.save(owner);

        Portfolio portfolio = new Portfolio();
        portfolio.setName("Private portfolio name");
        portfolio.setUser(owner);
        AssetMovement movement = new AssetMovement();
        movement.setPortfolio(portfolio);
        movement.setTickerSymbol("ABC");
        movement.setQuantity(new BigDecimal("4"));
        movement.setPrice(new BigDecimal("20"));
        movement.setDate(LocalDate.now());
        movement.setType(MovementType.BUY);
        portfolio.setAssetMovements(new ArrayList<>(List.of(movement)));
        portfolioId = portfolioRepository.save(portfolio).getId();
    }

    @Test
    @WithMockUser(username = "share-owner@example.com", roles = "USER")
    void createsReadOnlyRevocableLinkAndReturnsOnlyOptedInFields() throws Exception {
        String body = "{\"expiresAt\":\"" + Instant.now().plusSeconds(3600) + "\",\"includeValues\":false}";
        var created = mockMvc.perform(post("/v1/portfolio/{id}/shares", portfolioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.includeValues").value(false))
                .andReturn();

        JsonNode createdResponse = objectMapper.readTree(created.getResponse().getContentAsString());
        long shareId = createdResponse.get("id").asLong();
        String accessPath = createdResponse.get("accessPath").asText();
        String token = accessPath.substring(accessPath.lastIndexOf('/') + 1);
        PortfolioShare savedShare = shareRepository.findAll().getFirst();
        assertNotEquals(token, savedShare.getTokenHash());

        mockMvc.perform(get(accessPath))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.includesValues").value(false))
                .andExpect(jsonPath("$.assets[0].tickerSymbol").value("ABC"))
                .andExpect(jsonPath("$.assets[0].quantity").value(4))
                .andExpect(jsonPath("$.assets[0].unitPrice").doesNotExist())
                .andExpect(jsonPath("$.portfolioId").doesNotExist())
                .andExpect(jsonPath("$.assets[0].tickerFields").doesNotExist());

        mockMvc.perform(delete("/v1/portfolio/{id}/shares/{shareId}", portfolioId, shareId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(accessPath))
                .andExpect(status().isNotFound());
    }
}
