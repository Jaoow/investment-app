package dev.jaoow.investmentapp.infrastructure.client;

import com.fasterxml.jackson.databind.JsonNode;
import dev.jaoow.cotatrack.api.classification.AssetClassification;
import dev.jaoow.cotatrack.api.classification.AssetClassifier;
import dev.jaoow.cotatrack.api.tradingview.field.StockField;
import dev.jaoow.cotatrack.api.tradingview.field.enums.Market;
import dev.jaoow.cotatrack.api.tradingview.field.enums.Type;
import dev.jaoow.cotatrack.api.tradingview.screener.StockScreener;
import dev.jaoow.investmentapp.application.model.MarketTicker;
import dev.jaoow.investmentapp.application.service.MarketTickerProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Implementação de {@link MarketTickerProvider} que usa o TradingView Scanner
 * do CotaTrack para buscar e listar ações da B3.
 *
 * <p>Substitui o antigo BrapiMarketTickerProvider para não depender de token externo.
 */
@Slf4j
@Component
public class CotaTrackMarketTickerProvider implements MarketTickerProvider {

    private static final StockField[] SEARCH_FIELDS = {
            StockField.NAME,
            StockField.DESCRIPTION,
            StockField.TYPE,
            StockField.SUBTYPE,
            StockField.SECTOR,
            StockField.INDUSTRY,
            StockField.LOGOID,
    };

    private final StockScreener baseScreener;

    public CotaTrackMarketTickerProvider() {
        this.baseScreener = StockScreener.create()
                .withStockFields(SEARCH_FIELDS)
                .withTypes(Type.values())
                .withMarkets(Market.BRAZIL)
                .withSortBy(StockField.MARKET_CAPITALIZATION, false);
    }

    @Override
    public List<MarketTicker> searchTickers(String query, String type) {
        try {
            StockScreener screener = baseScreener.startIsolatedQuery();

            if (query != null && !query.isBlank()) {
                screener.withSearch(query.trim());
            }

            if (type != null && !type.isBlank()) {
                Type tvType = resolveType(type);
                if (tvType == Type.STOCK) {
                    // On B3, Units (e.g. BPAC11, TAEE11) are tagged as "fund" by TradingView.
                    // Include both STOCK and FUND so Units are not excluded.
                    screener.withTypes(Type.STOCK, Type.FUND);
                } else if (tvType != null) {
                    screener.addFilter(StockField.TYPE,
                            dev.jaoow.cotatrack.api.tradingview.filter.Filter.Operator.EQUAL,
                            tvType.getValue());
                }
            }

            JsonNode data = screener.fetch();
            if (data == null || !data.isArray()) {
                return Collections.emptyList();
            }

            List<MarketTicker> results = new ArrayList<>();
            for (JsonNode row : data) {
                MarketTicker ticker = mapToMarketTicker(row);
                if (ticker != null) {
                    results.add(ticker);
                }
            }
            return results;

        } catch (Exception ex) {
            log.warn("CotaTrack ticker search failed for query='{}', type='{}': {}", query, type, ex.getMessage());
            return Collections.emptyList();
        }
    }

    private MarketTicker mapToMarketTicker(JsonNode row) {
        if (row == null) return null;

        // Symbol in TradingView format is like "BMFBOVESPA:PETR4"
        String rawSymbol = getTextOrNull(row, "symbol");
        if (rawSymbol == null) return null;

        // Strip the exchange prefix (e.g. "BMFBOVESPA:PETR4" → "PETR4")
        String symbol = rawSymbol.contains(":") ? rawSymbol.substring(rawSymbol.indexOf(':') + 1) : rawSymbol;

        String name = getTextOrNull(row, StockField.NAME.getKey());
        String description = getTextOrNull(row, StockField.DESCRIPTION.getKey());
        String assetType = getTextOrNull(row, StockField.TYPE.getKey());
        String subType = getTextOrNull(row, StockField.SUBTYPE.getKey());
        String sector = getTextOrNull(row, StockField.SECTOR.getKey());
        String industry = getTextOrNull(row, StockField.INDUSTRY.getKey());
        String logoId = getTextOrNull(row, StockField.LOGOID.getKey());

        String logoUrl = (logoId != null && !logoId.isBlank())
                ? "https://s3-symbol-logo.tradingview.com/" + logoId + ".svg"
                : null;

        AssetClassification classification = AssetClassifier.classify(
                symbol, assetType, subType, sector, industry, description);

        return new MarketTicker(
                symbol,
                name,
                description,
                classification.getType(),
                classification.getSubType(),
                classification.getSector(),
                null,  // subsector not available from TradingView screener
                classification.getIndustry(),
                classification.getCategoryName(),
                logoUrl
        );
    }

    private String getTextOrNull(JsonNode node, String field) {
        JsonNode child = node.get(field);
        if (child == null || child.isNull() || child.isMissingNode()) return null;
        String text = child.asText(null);
        return (text == null || text.isBlank() || "null".equals(text)) ? null : text;
    }

    private Type resolveType(String type) {
        return Arrays.stream(Type.values())
                .filter(t -> t.getValue().equalsIgnoreCase(type) || t.name().equalsIgnoreCase(type))
                .findFirst()
                .orElse(null);
    }
}
