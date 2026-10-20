package dev.jaoow.investmentapp.infrastructure.web;

import dev.jaoow.cotatrack.api.QuoteQuery;
import dev.jaoow.cotatrack.api.requests.quotes.model.Quote;
import dev.jaoow.investmentapp.application.exception.MarketDataUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;

/**
 * Expõe cotações de mercado via CotaTrack (multi-provedor: TradingView → Yahoo → Brapi).
 */
@Slf4j
@RestController
@RequestMapping("/v1/quotes")
public class QuoteController {

    /**
     * Retorna cotação para um ou múltiplos tickers (separados por vírgula).
     *
     * <p>Exemplos:
     * <ul>
     *   <li>{@code GET /v1/quotes?ticker=PETR4}</li>
     *   <li>{@code GET /v1/quotes?ticker=PETR4,VALE3,HGLG11}</li>
     *   <li>{@code GET /v1/quotes?ticker=PETR4&dividends=true}</li>
     * </ul>
     */
    @GetMapping
    public Object getQuote(
            @RequestParam String ticker,
            @RequestParam(required = false) Boolean dividends) {

        boolean tickers = ticker.contains(",");

        try {
            QuoteQuery.QuoteQueryBuilder builder = QuoteQuery.builder()
                    .includeDividends(Boolean.TRUE.equals(dividends));

            if (tickers) {
                String[] symbols = ticker.split(",");
                builder.symbols(symbols);
                Map<String, Quote> result = builder.build().fetchMultiple();
                if (result == null || result.isEmpty()) {
                    throw new MarketDataUnavailableException(
                            "All market data providers returned no quotes for: " + ticker);
                }
                return result;
            } else {
                builder.symbol(ticker.trim());
                Quote result = builder.build().fetch();
                if (result == null) {
                    throw new MarketDataUnavailableException(
                            "All market data providers returned no quote for: " + ticker);
                }
                return result;
            }

        } catch (IOException ex) {
            throw new MarketDataUnavailableException(
                    "I/O error fetching quote for " + ticker + ": " + ex.getMessage(), ex);
        }
    }
}
