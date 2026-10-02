package dev.jaoow.investmentapp.infrastructure.client;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
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
}
