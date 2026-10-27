package dev.jaoow.investmentapp.application.service.summary;

import dev.jaoow.investmentapp.application.dto.response.summary.AssetSummaryResponse;
import dev.jaoow.investmentapp.application.model.AssetConsolidation;
import dev.jaoow.investmentapp.application.service.consolidation.AssetConsolidationService;
import dev.jaoow.investmentapp.domain.entity.Portfolio;
import dev.jaoow.investmentapp.domain.repository.PortfolioRepository;
import dev.jaoow.investmentapp.domain.repository.TickerRepository;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PortfolioSummaryServiceTests {
    @ParameterizedTest
    @CsvSource({"200.00,225.00,12.50", "300.00,355.00,18.33",
            "200.00,175.00,-12.50", "0.0000,0.00,0"})
    void roundsOnlyAfterConvertingToPercentageAndHandlesScaledZero(
            BigDecimal invested, BigDecimal currentValue, BigDecimal expected) {
        PortfolioRepository portfolios = mock(PortfolioRepository.class);
        AssetSummaryService assets = mock(AssetSummaryService.class);
        AssetConsolidationService consolidations = mock(AssetConsolidationService.class);
        Portfolio portfolio = new Portfolio();
        List<AssetConsolidation> positions = List.of(
                new AssetConsolidation("TEST4", invested, BigDecimal.ONE));
        when(portfolios.findById(1L)).thenReturn(Optional.of(portfolio));
        when(consolidations.processAssetMovements(portfolio)).thenReturn(positions);
        when(assets.createAssetSummaries(positions)).thenReturn(List.of(
                AssetSummaryResponse.builder().currentValue(currentValue).build()));
        PortfolioSummaryService service = new PortfolioSummaryService(
                portfolios, assets, mock(TickerRepository.class), consolidations);

        assertEquals(0, expected.compareTo(service.getPortfolioSummary(1L, null).getPercentageChange()));
    }
}
