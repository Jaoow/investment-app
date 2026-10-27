package dev.jaoow.investmentapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.jaoow.investmentapp.application.dto.request.PositionAdjustmentRequest;
import dev.jaoow.investmentapp.application.service.movement.PositionAdjustmentService;
import dev.jaoow.investmentapp.application.service.movement.PositionLedger;
import dev.jaoow.investmentapp.domain.entity.*;
import dev.jaoow.investmentapp.domain.entity.user.User;
import dev.jaoow.investmentapp.domain.model.AssetCategory;
import dev.jaoow.investmentapp.domain.model.MovementType;
import dev.jaoow.investmentapp.domain.repository.*;
import dev.jaoow.investmentapp.domain.repository.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PositionAdjustmentIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired PortfolioRepository portfolios;
    @Autowired TickerRepository tickers;
    @Autowired AssetMovementRepository movements;
    @Autowired PositionAdjustmentService service;
    @Autowired PositionLedger ledger;
    @Autowired PlatformTransactionManager transactions;
    private Long portfolioId;
    private String email;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        email = "adjust-" + UUID.randomUUID() + "@example.com";
        User owner = new User();
        owner.setName("Adjustment test");
        owner.setEmail(email);
        owner.setPassword("unused");
        owner = users.save(owner);
        Portfolio portfolio = new Portfolio();
        portfolio.setName("Adjustments");
        portfolio.setUser(owner);
        portfolioId = portfolios.save(portfolio).getId();
        Ticker ticker = new Ticker();
        ticker.setSymbol("ADJ4");
        ticker.setCategory(AssetCategory.EQUITIES);
        tickers.save(ticker);
        today = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
    }

    @Test
    void adjustsCostAndQuantityWithoutSyntheticTradesAndContinuesConsolidating() throws Exception {
        trade("BUY", "10", "20", today.minusDays(1), "2").andExpect(status().isCreated());
        position().andExpect(jsonPath("$.quantity").value(10))
                .andExpect(jsonPath("$.totalCost").value(202)).andExpect(jsonPath("$.revision").value(1));
        adjust(request("12", "252", 1, UUID.randomUUID())).andExpect(status().isOk())
                .andExpect(jsonPath("$.revision").value(2));
        trade("SELL", "2", "30", today, "0").andExpect(status().isCreated());
        position().andExpect(jsonPath("$.quantity").value(10)).andExpect(jsonPath("$.totalCost").value(210));
        List<AssetMovement> history = movements.findAllByPortfolioId(portfolioId);
        assertEquals(3, history.size());
        AssetMovement adjustment = history.stream().filter(m -> m.getType() == MovementType.POSITION_ADJUSTMENT).findFirst().orElseThrow();
        assertNull(adjustment.getPrice());
        assertEquals(0, adjustment.getPreviousTotalCost().compareTo(new BigDecimal("202")));
        assertEquals(email, adjustment.getAdjustmentAuthor());
        mvc.perform(get("/v1/portfolio/{id}/movement", portfolioId).param("sort", "date,desc", "id,desc")
                        .with(user(email).roles("USER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[1].reason").value("Conferência do extrato"));
        trade("BUY", "2", "15", today, "1").andExpect(status().isCreated());
        position().andExpect(jsonPath("$.quantity").value(12)).andExpect(jsonPath("$.totalCost").value(241));
    }

    @Test
    void retriesReturnOriginalResultEvenAfterLaterWritesAndRejectChangedPayload() throws Exception {
        UUID id = UUID.randomUUID();
        var request = request("12", "252", 0, id);
        adjust(request).andExpect(status().isOk()).andExpect(jsonPath("$.revision").value(1));
        trade("BUY", "1", "10", today, "0").andExpect(status().isCreated());
        adjust(request).andExpect(status().isOk()).andExpect(jsonPath("$.quantity").value(12))
                .andExpect(jsonPath("$.revision").value(1));
        position().andExpect(jsonPath("$.quantity").value(13)).andExpect(jsonPath("$.revision").value(2));
        adjust(request("11", "252", 0, id)).andExpect(status().isConflict());
        assertEquals(2, movements.findAllByPortfolioId(portfolioId).size());
    }

    @Test
    void rejectsStaleRevisionInvalidZeroCostAndGenericAdjustmentPost() throws Exception {
        trade("BUY", "1", "10", today, "0").andExpect(status().isCreated());
        adjust(request("2", "20", 0, UUID.randomUUID())).andExpect(status().isConflict());
        adjust(request("0", "1", 1, UUID.randomUUID())).andExpect(status().isUnprocessableEntity());
        trade("POSITION_ADJUSTMENT", "1", "10", today, "0").andExpect(status().isUnprocessableEntity());
        adjust(request("-1", "0", 1, UUID.randomUUID())).andExpect(status().isBadRequest());
        adjust(request("1", "0.000000001", 1, UUID.randomUUID())).andExpect(status().isBadRequest());
        assertEquals(1, movements.findAllByPortfolioId(portfolioId).size());
    }

    @Test
    void supportsZeroCostAndClosingPositionWithoutResidualCost() throws Exception {
        adjust(request("3", "1", 0, UUID.randomUUID())).andExpect(status().isOk());
        trade("SELL", "3", "1", today, "0").andExpect(status().isCreated());
        position().andExpect(jsonPath("$.quantity").value(0)).andExpect(jsonPath("$.totalCost").value(0));
        adjust(request("2", "0", 2, UUID.randomUUID())).andExpect(status().isOk());
        position().andExpect(jsonPath("$.quantity").value(2)).andExpect(jsonPath("$.totalCost").value(0));
        adjust(request("0", "0", 3, UUID.randomUUID())).andExpect(status().isOk());
    }

    @Test
    void protectsAuditHistoryRetroactiveWritesAndSnapshotReplacement() throws Exception {
        adjust(request("10", "200", 0, UUID.randomUUID())).andExpect(status().isOk());
        Long adjustmentId = movements.findAllByPortfolioId(portfolioId).getFirst().getId();
        mvc.perform(delete("/v1/portfolio/{id}/movement/{mid}", portfolioId, adjustmentId).with(user(email).roles("USER")))
                .andExpect(status().isConflict());
        mvc.perform(put("/v1/portfolio/{id}/movement/{mid}", portfolioId, adjustmentId).with(user(email).roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON).content(tradeBody("BUY", "1", "10", today, "0")))
                .andExpect(status().isConflict());
        trade("BUY", "1", "10", today.minusDays(1), "0").andExpect(status().isConflict());
        byte[] sheet = new dev.jaoow.investmentapp.application.service.movement.AssetMovementTemplateService()
                .createPositionsTemplate();
        // A valid populated file reaches the protected replacement path.
        try (var workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook(new java.io.ByteArrayInputStream(sheet));
             var bytes = new java.io.ByteArrayOutputStream()) {
            var row = workbook.getSheetAt(0).createRow(1);
            row.createCell(0).setCellValue("ADJ4");
            row.createCell(1).setCellValue(10);
            row.createCell(2).setCellValue(20);
            workbook.write(bytes);
            mvc.perform(multipart("/v1/portfolio/{id}/movement/position-import", portfolioId)
                            .file(new MockMultipartFile("file", "position.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes.toByteArray()))
                            .with(user(email).roles("USER")))
                    .andExpect(status().isConflict());
        }
        position().andExpect(jsonPath("$.quantity").value(10)).andExpect(jsonPath("$.revision").value(1));
    }

    @Test
    void deniesOtherUsersAndAnonymousCallers() throws Exception {
        mvc.perform(get("/v1/portfolio/{id}/positions/ADJ4", portfolioId)).andExpect(status().isUnauthorized());
        mvc.perform(get("/v1/portfolio/{id}/positions/ADJ4", portfolioId).with(user("other@example.com").roles("USER")))
                .andExpect(status().isForbidden());
        mvc.perform(put("/v1/portfolio/{id}/positions/ADJ4", portfolioId).with(user("other@example.com").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(request("1", "1", 0, UUID.randomUUID()))))
                .andExpect(status().isForbidden());
    }

    @Test
    void rollsBackPersistedAdjustmentAndRevisionOnFailureThenAllowsRetry() {
        UUID id = UUID.randomUUID();
        var payload = request("12", "252", 0, id);
        var auth = new UsernamePasswordAuthenticationToken(email, "unused",
                List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(auth);
        try {
            assertThrows(IllegalStateException.class, () -> new TransactionTemplate(transactions).execute(status -> {
                service.adjust(portfolioId, "ADJ4", payload, () -> email);
                assertEquals(1, movements.findAllByPortfolioId(portfolioId).size());
                throw new IllegalStateException("Injected failure after flush");
            }));
            assertTrue(movements.findAllByPortfolioId(portfolioId).isEmpty());
            assertEquals(0, portfolios.findById(portfolioId).orElseThrow().getPositionRevision());
            assertEquals(1, service.adjust(portfolioId, "ADJ4", payload, () -> email).revision());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void serializesConcurrentAdjustmentsAndSales() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch start = new CountDownLatch(1);
            Callable<Integer> task = () -> {
                assertTrue(start.await(5, TimeUnit.SECONDS));
                return adjust(request("10", "200", 0, UUID.randomUUID())).andReturn().getResponse().getStatus();
            };
            Future<Integer> first = executor.submit(task);
            Future<Integer> second = executor.submit(task);
            start.countDown();
            List<Integer> statuses = List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS));
            assertTrue(statuses.contains(200));
            assertTrue(statuses.contains(409));
            CountDownLatch sellStart = new CountDownLatch(1);
            Callable<Integer> sell = () -> {
                assertTrue(sellStart.await(5, TimeUnit.SECONDS));
                return trade("SELL", "7", "20", today, "0").andReturn().getResponse().getStatus();
            };
            first = executor.submit(sell);
            second = executor.submit(sell);
            sellStart.countDown();
            statuses = List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS));
            assertTrue(statuses.contains(201));
            assertTrue(statuses.contains(422));
            position().andExpect(jsonPath("$.quantity").value(3));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void concurrentRetriesPersistOnlyOneAdjustment() throws Exception {
        var payload = request("10", "200", 0, UUID.randomUUID());
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch start = new CountDownLatch(1);
            Callable<Long> retry = () -> {
                assertTrue(start.await(5, TimeUnit.SECONDS));
                String body = adjust(payload).andExpect(status().isOk())
                        .andExpect(jsonPath("$.quantity").value(10))
                        .andExpect(jsonPath("$.totalCost").value(200))
                        .andReturn().getResponse().getContentAsString();
                return json.readTree(body).get("revision").asLong();
            };
            Future<Long> first = executor.submit(retry);
            Future<Long> second = executor.submit(retry);
            start.countDown();
            assertEquals(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS));
            assertEquals(1, movements.findAllByPortfolioId(portfolioId).size());
            position().andExpect(jsonPath("$.revision").value(1));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void refreshesRevisionLoadedBeforeAnotherTransactionCommits() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            new TransactionTemplate(transactions).executeWithoutResult(status -> {
                assertEquals(0, portfolios.findById(portfolioId).orElseThrow().getPositionRevision());
                Future<Integer> write = executor.submit(() ->
                        trade("BUY", "1", "10", today, "0").andReturn().getResponse().getStatus());
                try {
                    assertEquals(201, write.get(15, TimeUnit.SECONDS));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(e);
                } catch (ExecutionException | TimeoutException e) {
                    throw new AssertionError(e);
                }
                assertEquals(1, ledger.lock(portfolioId).getPositionRevision());
            });
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void rejectsAdjustmentBeforeFutureTradesWithoutChangingHistory() throws Exception {
        AssetMovement futureImport = new AssetMovement();
        futureImport.setPortfolio(portfolios.findById(portfolioId).orElseThrow());
        futureImport.setTickerSymbol("ADJ4");
        futureImport.setType(MovementType.BUY);
        futureImport.setQuantity(BigDecimal.ONE);
        futureImport.setPrice(BigDecimal.TEN);
        futureImport.setDate(today.plusDays(1));
        movements.saveAndFlush(futureImport);
        adjust(request("2", "20", 0, UUID.randomUUID())).andExpect(status().isConflict());
        position().andExpect(jsonPath("$.quantity").value(1)).andExpect(jsonPath("$.revision").value(0));
        assertEquals(1, movements.findAllByPortfolioId(portfolioId).size());
    }

    @Test
    void rejectsLegacyEditsAndDeletesThatInvalidateSubsequentSales() throws Exception {
        trade("BUY", "10", "20", today.minusDays(1), "0").andExpect(status().isCreated());
        Long buyId = movements.findAllByPortfolioId(portfolioId).getFirst().getId();
        trade("SELL", "7", "30", today, "0").andExpect(status().isCreated());
        mvc.perform(put("/v1/portfolio/{id}/movement/{mid}", portfolioId, buyId).with(user(email).roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON).content(tradeBody("BUY", "5", "20", today.minusDays(1), "0")))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(delete("/v1/portfolio/{id}/movement/{mid}", portfolioId, buyId).with(user(email).roles("USER")))
                .andExpect(status().isUnprocessableEntity());
        position().andExpect(jsonPath("$.quantity").value(3)).andExpect(jsonPath("$.totalCost").value(60))
                .andExpect(jsonPath("$.revision").value(2));
    }

    @Test
    void preservesExactPreviousCostOfFractionalHoldingsInAudit() throws Exception {
        trade("BUY", "3.00000001", "0.0001", today, "0").andExpect(status().isCreated());
        adjust(request("3", "0.0003", 1, UUID.randomUUID())).andExpect(status().isOk());
        AssetMovement adjustment = movements.findAllByPortfolioId(portfolioId).stream()
                .filter(m -> m.getType() == MovementType.POSITION_ADJUSTMENT).findFirst().orElseThrow();
        assertEquals(0, adjustment.getPreviousTotalCost().compareTo(new BigDecimal("0.000300000001")));
    }

    @Test
    void preservesTinyRemainingCostAfterPartialSaleAndAnotherAdjustment() throws Exception {
        adjust(request("3", "0.00000002", 0, UUID.randomUUID())).andExpect(status().isOk());
        trade("SELL", "2.9", "1", today, "0").andExpect(status().isCreated());
        adjust(request("0.1", "0.01", 2, UUID.randomUUID())).andExpect(status().isOk());
        AssetMovement latest = movements.findAllByPortfolioId(portfolioId).stream()
                .filter(m -> m.getType() == MovementType.POSITION_ADJUSTMENT)
                .max(java.util.Comparator.comparing(AssetMovement::getId)).orElseThrow();
        assertEquals(0, latest.getPreviousTotalCost().compareTo(new BigDecimal("0.0000000006666667")));
    }

    private PositionAdjustmentRequest request(String quantity, String cost, long revision, UUID id) {
        return new PositionAdjustmentRequest(new BigDecimal(quantity), new BigDecimal(cost), revision, id, "Conferência do extrato");
    }

    private ResultActions adjust(PositionAdjustmentRequest request) throws Exception {
        return mvc.perform(put("/v1/portfolio/{id}/positions/ADJ4", portfolioId).with(user(email).roles("USER"))
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(request)));
    }

    private ResultActions position() throws Exception {
        return mvc.perform(get("/v1/portfolio/{id}/positions/ADJ4", portfolioId).with(user(email).roles("USER")))
                .andExpect(status().isOk());
    }

    private ResultActions trade(String type, String quantity, String price, LocalDate date, String cost) throws Exception {
        return mvc.perform(post("/v1/portfolio/{id}/movement", portfolioId).with(user(email).roles("USER"))
                .contentType(MediaType.APPLICATION_JSON).content(tradeBody(type, quantity, price, date, cost)));
    }

    private String tradeBody(String type, String quantity, String price, LocalDate date, String cost) {
        return """
                {"tickerSymbol":"ADJ4","quantity":%s,"price":%s,"type":"%s","date":"%s","operationCost":%s}
                """.formatted(quantity, price, type, date, cost);
    }
}
