package dev.jaoow.investmentapp.infrastructure.client;

import dev.jaoow.investmentapp.application.dto.response.BrapiQuoteDto;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Optional;
import java.time.Instant;

@Component
public class BrapiClient {
    private final RestTemplate restTemplate;

    @Value("${brapi.api.token}")
    private String apiToken;

    public BrapiClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Cacheable(
            value = "quotes",
            key = "#ticker + ':' + #range + ':' + #interval + ':' + #fundamental + ':' + #dividends",
            cacheManager = "cacheManagerWithRefresh",
            unless="#result == null"
    )
    public Optional<BrapiQuoteDto> getQuote(String ticker, String range, String interval, Boolean fundamental, Boolean dividends) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromHttpUrl("https://brapi.dev/api/v2/stocks/quote")
                .queryParam("symbols", ticker);

        if (range != null && !range.isEmpty()) {
            uriBuilder.queryParam("range", range);
        }

        if (interval != null && !interval.isEmpty()) {
            uriBuilder.queryParam("interval", interval);
        }

        if (fundamental != null) {
            uriBuilder.queryParam("fundamental", fundamental);
        }

        if (dividends != null) {
            uriBuilder.queryParam("dividends", dividends);
        }

        HttpHeaders headers = new HttpHeaders();
        if (apiToken != null && !apiToken.isBlank()) {
            headers.setBearerAuth(apiToken);
        }

        BrapiResponse response = restTemplate.exchange(
                uriBuilder.build().encode().toUri(),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                BrapiResponse.class
        ).getBody();
        if (response != null && response.getResults() != null && !response.getResults().isEmpty()) {
            BrapiQuoteDto quote = response.getResults().getFirst().getData();
            if (quote == null) {
                return Optional.empty();
            }
            quote.setSymbol(response.getResults().getFirst().getSymbol());
            quote.setFetchedAt(Instant.now());
            return Optional.of(quote);
        }

        return Optional.empty();
    }

    @Setter
    @Getter
    private static class BrapiResponse {
        private List<BrapiResult> results;
    }

    @Setter
    @Getter
    private static class BrapiResult {
        private String symbol;
        private BrapiQuoteDto data;
    }
}
