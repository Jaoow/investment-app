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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PortfolioAuthorizationIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PortfolioRepository portfolioRepository;

    private Long ownerPortfolioId;

    @BeforeEach
    void setUp() {
        portfolioRepository.deleteAll();
        userRepository.deleteAll();

        User owner = userRepository.save(createUser("owner@example.com"));
        User other = userRepository.save(createUser("other@example.com"));

        Portfolio ownerPortfolio = new Portfolio();
        ownerPortfolio.setName("Owner portfolio");
        ownerPortfolio.setUser(owner);
        ownerPortfolioId = portfolioRepository.save(ownerPortfolio).getId();

        Portfolio otherPortfolio = new Portfolio();
        otherPortfolio.setName("Other portfolio");
        otherPortfolio.setUser(other);
        portfolioRepository.save(otherPortfolio);
    }

    @Test
    @WithMockUser(username = "other@example.com", roles = "USER")
    void rejectsAccessToAnotherUsersPortfolioData() throws Exception {
        mockMvc.perform(get("/v1/portfolio/{id}/summary", ownerPortfolioId))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/v1/portfolio/{id}/allocations", ownerPortfolioId))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/v1/portfolio/{id}/rebalance", ownerPortfolioId))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/v1/portfolio/{id}", ownerPortfolioId)
                        .contentType("application/json")
                        .content("{\"name\":\"Stolen portfolio\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/v1/portfolio/{id}", ownerPortfolioId))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "owner@example.com", roles = "USER")
    void allowsOwnerToReadPortfolioViews() throws Exception {
        mockMvc.perform(get("/v1/portfolio/{id}/summary", ownerPortfolioId))
                .andExpect(status().isOk());

        mockMvc.perform(get("/v1/portfolio/{id}/allocations", ownerPortfolioId))
                .andExpect(status().isOk());

        mockMvc.perform(get("/v1/portfolio/{id}/rebalance", ownerPortfolioId))
                .andExpect(status().isOk());
    }

    private User createUser(String email) {
        User user = new User();
        user.setName(email);
        user.setEmail(email);
        user.setPassword("not-used-in-this-test");
        return user;
    }
}
