package dev.jaoow.investmentapp.infrastructure.client;

import dev.jaoow.investmentapp.application.dto.response.BrapiTickerListResponse;
import dev.jaoow.investmentapp.application.dto.response.BrapiTickerListItemDto;
import dev.jaoow.investmentapp.application.model.MarketTicker;
import dev.jaoow.investmentapp.application.service.MarketTickerProvider;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class BrapiMarketTickerProvider implements MarketTickerProvider {

    private final BrapiClient brapiClient;

    public BrapiMarketTickerProvider(BrapiClient brapiClient) {
        this.brapiClient = brapiClient;
    }

    @Override
    public List<MarketTicker> searchTickers(String query, String type) {
        Optional<BrapiTickerListResponse> response = brapiClient.searchTickers(query, type);
        
        if (response.isEmpty() || response.get().getResults() == null) {
            return Collections.emptyList();
        }

        return response.get().getResults().stream()
                .map(this::mapToMarketTicker)
                .collect(Collectors.toList());
    }

    private MarketTicker mapToMarketTicker(BrapiTickerListItemDto dto) {
        return new MarketTicker(
                dto.getSymbol(),
                dto.getName(),
                dto.getLongName(),
                dto.getAssetType(),
                dto.getSubType(),
                dto.getSector(),
                dto.getSubsector(),
                dto.getLogoUrl()
        );
    }
}
