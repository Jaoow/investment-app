package dev.jaoow.investmentapp.application.service;

import dev.jaoow.investmentapp.application.dto.response.PortfolioConcentrationResponse.Dimension;
import dev.jaoow.investmentapp.application.dto.response.PortfolioConcentrationResponse.Level;
import dev.jaoow.investmentapp.application.dto.response.summary.AssetSummaryResponse;
import dev.jaoow.investmentapp.application.dto.response.summary.PortfolioSummaryResponse;
import dev.jaoow.investmentapp.application.model.TickerFields;
import dev.jaoow.investmentapp.application.service.summary.PortfolioSummaryService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PortfolioConcentrationTests {

    @Test
    void exposesConcentrationByAssetSectorAndClassBehindAnApparentlyDiversifiedPortfolio() {
        PortfolioSummaryService summaryService = mock(PortfolioSummaryService.class);
        when(summaryService.getPortfolioSummary(1L, null)).thenReturn(PortfolioSummaryResponse.builder()
                .portfolioId(1L)
                .currentValue(new BigDecimal("1000"))
                .assetSummaries(List.of(
                        asset("WEGE3", "200", "EQUITIES", "Bens Industriais"),
                        asset("ITUB4", "400", "EQUITIES", "Financeiro"),
                        asset("HGLG11", "100", "REAL_ESTATE_FUNDS", null),
                        asset("BBDC4", "300", "EQUITIES", "Financeiro")))
                .build());
        PortfolioAnalyticsService service = new PortfolioAnalyticsService(summaryService, mock(AllocationService.class));

        var concentration = service.getConcentration(1L);

        assertEquals(4, concentration.assetsCount());
        assertEquals(new BigDecimal("3.3"), concentration.effectiveAssets());
        assertEquals(new BigDecimal("40.00"), concentration.topAssetPercentage());
        assertEquals(new BigDecimal("90.00"), concentration.topThreeAssetsPercentage());
        assertEquals(Level.CRITICAL, concentration.level());

        assertEquals(List.of("ITUB4", "BBDC4", "WEGE3", "HGLG11"),
                concentration.assets().stream().map(slice -> slice.key()).toList());
        assertEquals(Level.WARNING, concentration.assets().get(2).level());
        assertEquals(Level.OK, concentration.assets().get(3).level());

        var financials = concentration.sectors().getFirst();
        assertEquals("Financeiro", financials.label());
        assertEquals(new BigDecimal("70.00"), financials.percentage());
        assertEquals(List.of("ITUB4", "BBDC4"), financials.tickers());
        assertEquals(Level.OK, concentration.sectors().getLast().level());
        assertEquals("Setor não informado", concentration.sectors().getLast().label());

        assertEquals("equities", concentration.classes().getFirst().key());
        assertEquals(new BigDecimal("90.00"), concentration.classes().getFirst().percentage());

        assertEquals(5, concentration.alerts().size());
        assertEquals(Dimension.CLASS, concentration.alerts().get(0).dimension());
        assertEquals(Dimension.SECTOR, concentration.alerts().get(1).dimension());
        assertEquals("O setor Financeiro soma 70,0% do patrimônio em 2 ativos (referência: até 40,0% por setor).",
                concentration.alerts().get(1).message());
        assertEquals(Level.WARNING, concentration.alerts().getLast().level());
    }

    private AssetSummaryResponse asset(String ticker, String value, String category, String sector) {
        TickerFields fields = new TickerFields();
        fields.setCategory(category);
        fields.setSector(sector);
        return AssetSummaryResponse.builder()
                .tickerSymbol(ticker)
                .quantity(BigDecimal.ONE)
                .totalInvested(new BigDecimal(value))
                .currentValue(new BigDecimal(value))
                .tickerFields(fields)
                .build();
    }
}
