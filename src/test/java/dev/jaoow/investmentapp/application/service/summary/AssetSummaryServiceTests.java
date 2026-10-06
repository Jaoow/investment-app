package dev.jaoow.investmentapp.application.service.summary;

import dev.jaoow.investmentapp.application.exception.MarketDataUnavailableException;
import dev.jaoow.investmentapp.application.dto.response.BrapiQuoteDto;
import dev.jaoow.investmentapp.application.model.AssetConsolidation;
import dev.jaoow.investmentapp.domain.repository.TickerRepository;
import dev.jaoow.investmentapp.infrastructure.client.BrapiClient;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class AssetSummaryServiceTests {

    @Test
    void rejectsMissingQuoteInsteadOfValuingPositionAtZero() {
        TickerRepository tickerRepository = mock(TickerRepository.class);
        BrapiClient brapiClient = new BrapiClient(null) {
            @Override
            public Optional<BrapiQuoteDto> getQuote(String ticker, String range, String interval, Boolean fundamental, Boolean dividends) {
                return Optional.empty();
            }
        };
        ModelMapper modelMapper = new ModelMapper();
        AssetSummaryService service = new AssetSummaryService(tickerRepository, brapiClient, modelMapper);
        AssetConsolidation position = new AssetConsolidation("ABC", new BigDecimal("100.00"), new BigDecimal("2"));

        assertThrows(MarketDataUnavailableException.class, () -> service.createAssetSummaries(List.of(position)));
    }
}