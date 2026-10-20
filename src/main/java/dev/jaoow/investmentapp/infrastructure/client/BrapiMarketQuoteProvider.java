package dev.jaoow.investmentapp.infrastructure.client;

import dev.jaoow.investmentapp.application.exception.MarketDataUnavailableException;
import dev.jaoow.investmentapp.application.model.MarketQuote;
import dev.jaoow.investmentapp.application.service.MarketQuoteProvider;

/**
 * @deprecated Substituído por {@link CotaTrackMarketQuoteProvider}.
 *             Mantido apenas para referência histórica. Não é um Spring Bean ativo.
 */
@Deprecated(forRemoval = false)
public class BrapiMarketQuoteProvider implements MarketQuoteProvider {

    @Override
    public MarketQuote getQuote(String tickerSymbol) {
        throw new MarketDataUnavailableException(
                "BrapiMarketQuoteProvider is deprecated. Use CotaTrackMarketQuoteProvider.");
    }
}
