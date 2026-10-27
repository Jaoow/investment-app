package dev.jaoow.investmentapp;

import dev.jaoow.investmentapp.infrastructure.web.RequestObservabilityFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class HealthIntegrationTests {
    @Autowired private MockMvc mvc;

    @Test
    @WithMockUser(roles = "USER")
    void authenticatedHealthReturnsMinimalStatusAndCorrelationId() throws Exception {
        String id = "12345678-1234-1234-1234-123456789abc";
        mvc.perform(get("/v1/health").header(RequestObservabilityFilter.HEADER, id))
                .andExpect(status().isOk())
                .andExpect(header().string(RequestObservabilityFilter.HEADER, id))
                .andExpect(content().json("{\"status\":\"UP\"}", true));
    }

    @Test
    void healthPreservesAuthenticationRequirementAndCorrelatesUnauthorizedRequests() throws Exception {
        mvc.perform(get("/v1/health"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists(RequestObservabilityFilter.HEADER));
    }
}
