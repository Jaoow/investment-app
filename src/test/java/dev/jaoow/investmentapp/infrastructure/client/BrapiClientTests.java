package dev.jaoow.investmentapp.infrastructure.client;

import dev.jaoow.investmentapp.application.exception.MarketDataUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import static org.springframework.http.MediaType.APPLICATION_JSON;

class BrapiClientTests {

    @Test
    void requestsV2QuoteWithBearerAuthAndNormalizesNestedResult() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        BrapiClient client = new BrapiClient(restTemplate);
        ReflectionTestUtils.setField(client, "apiToken", "test-api-key");

        server.expect(requestTo("https://brapi.dev/api/v2/stocks/quote?symbols=ABC"))
                .andExpect(header("Authorization", "Bearer test-api-key"))
                .andRespond(withSuccess("""
                        {
                          "results": [{
                            "symbol": "ABC",
                            "data": {
                              "currency": "BRL",
                              "regularMarketPrice": 88.00,
                              "regularMarketChangePercent": -3.00,
                              "regularMarketTime": "2026-10-02T14:00:00.000Z"
                            }
                          }],
                          "requestedAt": "2026-10-02T14:00:01.000Z"
                        }
                        """, APPLICATION_JSON));

        var quote = client.getQuote("ABC", null, null, null, null).orElseThrow();

        assertEquals("ABC", quote.getSymbol());
        assertEquals("BRL", quote.getCurrency());
        assertEquals("2026-10-02T14:00:00.000Z", quote.getRegularMarketTime());
        assertNotNull(quote.getFetchedAt());
        server.verify();
    }

      @Test
      void translatesProviderConcurrencyLimitIntoMarketDataUnavailable() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        BrapiClient client = new BrapiClient(restTemplate);

        server.expect(requestTo("https://brapi.dev/api/v2/stocks/quote?symbols=ABC"))
            .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                .contentType(APPLICATION_JSON)
                .body("{\"code\":\"RATE_LIMITED\",\"details\":{\"concurrencyLimit\":1}}"));

        assertThrows(MarketDataUnavailableException.class,
            () -> client.getQuote("ABC", null, null, null, null));
        server.verify();
      }

      @Test
      void serializesDifferentTickerRequestsForSingleConcurrencyProvider() throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        BrapiClient client = new BrapiClient(restTemplate);
        AtomicInteger inFlight = new AtomicInteger();
        AtomicInteger peakInFlight = new AtomicInteger();

        server.expect(ExpectedCount.twice(), request -> { })
            .andRespond(request -> {
              int currentInFlight = inFlight.incrementAndGet();
              peakInFlight.accumulateAndGet(currentInFlight, Math::max);
              try {
                try {
                  Thread.sleep(50);
                } catch (InterruptedException ex) {
                  Thread.currentThread().interrupt();
                  throw new IOException(ex);
                }
                return withSuccess("""
                    {"results":[{"symbol":"ABC","data":{"currency":"BRL","regularMarketPrice":10,"regularMarketTime":"2026-10-06T10:00:00Z"}}]}
                    """, APPLICATION_JSON).createResponse(request);
              } finally {
                inFlight.decrementAndGet();
              }
            });

        var executor = Executors.newFixedThreadPool(2);
        try {
          var first = executor.submit(() -> client.getQuote("ABC", null, null, null, null));
          var second = executor.submit(() -> client.getQuote("XYZ", null, null, null, null));
          assertTrue(first.get(5, TimeUnit.SECONDS).isPresent());
          assertTrue(second.get(5, TimeUnit.SECONDS).isPresent());
        } finally {
          executor.shutdownNow();
        }

        assertEquals(1, peakInFlight.get());
        server.verify();
      }
}
