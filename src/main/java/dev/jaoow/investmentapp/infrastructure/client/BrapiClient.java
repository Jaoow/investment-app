package dev.jaoow.investmentapp.infrastructure.client;

import dev.jaoow.investmentapp.application.dto.response.BrapiQuoteDto;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Optional;

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
            key = "#ticker",
            cacheManager = "cacheManagerWithRefresh",
            unless="#result == null"
    )
    public Optional<BrapiQuoteDto> getQuote(String ticker, String range, String interval, Boolean fundamental, Boolean dividends) {
        String baseUrl = "https://brapi.dev/api/quote/{ticker}";
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .queryParam("token", apiToken);

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

        String url = uriBuilder.buildAndExpand(ticker).toUriString();

        BrapiResponse response = restTemplate.getForObject(url, BrapiResponse.class);
        if (response != null && response.getResults() != null && !response.getResults().isEmpty()) {
            return Optional.of(response.getResults().getFirst());
        }

        return Optional.empty();
    }

    @Setter
    @Getter
    private static class BrapiResponse {
        private List<BrapiQuoteDto> results;
    }
}
