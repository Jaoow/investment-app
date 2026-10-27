package dev.jaoow.investmentapp.application.service.summary;

import dev.jaoow.cotatrack.api.requests.quotes.model.Quote;
import dev.jaoow.investmentapp.application.dto.response.BrapiTickerResolveResultDto;
import dev.jaoow.investmentapp.application.dto.response.summary.AssetSummaryResponse;
import dev.jaoow.investmentapp.application.model.AssetConsolidation;
import dev.jaoow.investmentapp.application.model.MarketTicker;
import dev.jaoow.investmentapp.application.service.MarketTickerProvider;
import dev.jaoow.investmentapp.application.service.QuoteService;
import dev.jaoow.investmentapp.domain.entity.Ticker;
import dev.jaoow.investmentapp.domain.model.AssetCategory;
import dev.jaoow.investmentapp.domain.repository.TickerRepository;
import dev.jaoow.investmentapp.infrastructure.client.BrapiClient;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AssetSummaryServiceTests {

    @Test
    void handlesMissingQuoteWithZeroPrice() {
        TickerRepository tickerRepository = mock(TickerRepository.class);
        QuoteService quoteService = mock(QuoteService.class);
        BrapiClient brapiClient = mock(BrapiClient.class);
        MarketTickerProvider marketTickerProvider = mock(MarketTickerProvider.class);

        when(quoteService.findQuotes(List.of("ABC"))).thenReturn(Map.of());
        when(brapiClient.resolveTickers(List.of("ABC"))).thenReturn(List.of());
        when(brapiClient.getQuote("ABC", null, null, null, null)).thenReturn(Optional.empty());

        ModelMapper modelMapper = new ModelMapper();
        AssetSummaryService service = new AssetSummaryService(
                tickerRepository, quoteService, brapiClient, marketTickerProvider, modelMapper);
        AssetConsolidation position = new AssetConsolidation("ABC", new BigDecimal("100.00"), new BigDecimal("2"));

        List<AssetSummaryResponse> summaries = service.createAssetSummaries(List.of(position));

        assertEquals(1, summaries.size());
        AssetSummaryResponse summary = summaries.get(0);
        assertEquals("ABC", summary.getTickerSymbol());
        assertEquals(BigDecimal.ZERO, summary.getCurrentValue());
        assertEquals(new BigDecimal("-100.00"), summary.getProfitOrLoss());
    }

    @Test
    void createsAssetSummaryWithValidQuote() {
        TickerRepository tickerRepository = mock(TickerRepository.class);
        QuoteService quoteService = mock(QuoteService.class);
        BrapiClient brapiClient = mock(BrapiClient.class);
        MarketTickerProvider marketTickerProvider = mock(MarketTickerProvider.class);

        Quote quote = Quote.builder()
                .symbol("PETR4")
                .regularMarketPrice(35.50)
                .build();
        when(quoteService.findQuotes(List.of("PETR4"))).thenReturn(Map.of("PETR4", quote));

        Ticker ticker = new Ticker();
        ticker.setSymbol("PETR4");
        ticker.setCategory(AssetCategory.EQUITIES);
        when(tickerRepository.findAllById(List.of("PETR4"))).thenReturn(List.of(ticker));

        ModelMapper modelMapper = new ModelMapper();
        AssetSummaryService service = new AssetSummaryService(
                tickerRepository, quoteService, brapiClient, marketTickerProvider, modelMapper);
        AssetConsolidation position = new AssetConsolidation("PETR4", new BigDecimal("300.00"), new BigDecimal("10"));

        List<AssetSummaryResponse> summaries = service.createAssetSummaries(List.of(position));

        assertEquals(1, summaries.size());
        AssetSummaryResponse summary = summaries.get(0);
        assertEquals("PETR4", summary.getTickerSymbol());
        assertEquals(0, new BigDecimal("355.00").compareTo(summary.getCurrentValue()));
        assertEquals(0, new BigDecimal("55.00").compareTo(summary.getProfitOrLoss()));
        assertEquals(0, new BigDecimal("18.33").compareTo(summary.getPercentageChange()));
        assertNotNull(summary.getTickerFields());
        assertEquals("EQUITIES", summary.getTickerFields().getCategory());
    }

    @Test
    void handlesZeroCostPositionsWithDecimalScale() {
        TickerRepository tickers = mock(TickerRepository.class);
        QuoteService quotes = mock(QuoteService.class);
        AssetSummaryService service = new AssetSummaryService(
                tickers, quotes, mock(BrapiClient.class), mock(MarketTickerProvider.class), new ModelMapper());
        AssetConsolidation position = new AssetConsolidation(
                "ZERO3", new BigDecimal("0.0000"), new BigDecimal("1.00000000"));

        AssetSummaryResponse summary = service.createAssetSummariesWithoutQuotes(List.of(position)).getFirst();

        assertEquals(0, BigDecimal.ZERO.compareTo(summary.getPercentageChange()));
        assertEquals(0, BigDecimal.ZERO.compareTo(summary.getAveragePrice()));
        verifyNoInteractions(quotes);
    }

    @Test
    void resolvesTickerViaBrapiWhenInitialQuoteFails() {
        TickerRepository tickerRepository = mock(TickerRepository.class);
        QuoteService quoteService = mock(QuoteService.class);
        BrapiClient brapiClient = mock(BrapiClient.class);
        MarketTickerProvider marketTickerProvider = mock(MarketTickerProvider.class);

        // Initial ticker BIDI4 has no quote
        when(quoteService.findQuotes(List.of("BIDI4"))).thenReturn(Map.of());

        // Brapi resolves BIDI4 -> INBR32
        BrapiTickerResolveResultDto resolveResult = new BrapiTickerResolveResultDto();
        resolveResult.setSymbol("INBR32");
        resolveResult.setChanged(true);
        when(brapiClient.resolveTickers(List.of("BIDI4"))).thenReturn(List.of(resolveResult));

        // Quote for new ticker INBR32 is available
        Quote inbrQuote = Quote.builder()
                .symbol("INBR32")
                .regularMarketPrice(42.00)
                .build();
        when(quoteService.findQuote("INBR32")).thenReturn(Optional.of(inbrQuote));

        ModelMapper modelMapper = new ModelMapper();
        AssetSummaryService service = new AssetSummaryService(
                tickerRepository, quoteService, brapiClient, marketTickerProvider, modelMapper);
        AssetConsolidation position = new AssetConsolidation("BIDI4", new BigDecimal("200.00"), new BigDecimal("5"));

        List<AssetSummaryResponse> summaries = service.createAssetSummaries(List.of(position));

        assertEquals(1, summaries.size());
        AssetSummaryResponse summary = summaries.get(0);
        assertEquals("BIDI4", summary.getTickerSymbol());
        assertEquals(0, new BigDecimal("210.00").compareTo(summary.getCurrentValue()));
        assertEquals(0, new BigDecimal("10.00").compareTo(summary.getProfitOrLoss()));
    }

    @Test
    void autoDiscoversMissingTickerAndInfersCategoryEquitiesForUnits() {
        TickerRepository tickerRepository = mock(TickerRepository.class);
        QuoteService quoteService = mock(QuoteService.class);
        BrapiClient brapiClient = mock(BrapiClient.class);
        MarketTickerProvider marketTickerProvider = mock(MarketTickerProvider.class);

        Quote quote = Quote.builder()
                .symbol("BPAC11")
                .regularMarketPrice(80.00)
                .shortName("BPAC11")
                .longName("Banco BTG Pactual SA Units Cons of 1 Sh + 2 Pfd Shs A")
                .build();
        when(quoteService.findQuotes(List.of("BPAC11"))).thenReturn(Map.of("BPAC11", quote));

        when(tickerRepository.findAllById(List.of("BPAC11"))).thenReturn(List.of());

        MarketTicker tvDetail = new MarketTicker(
                "BPAC11", "BPAC11", "Banco BTG Pactual SA Units Cons of 1 Sh + 2 Pfd Shs A",
                "fund", "etf", "Finance", null, "https://logo.svg"
        );
        when(marketTickerProvider.searchTickers("BPAC11", null)).thenReturn(List.of(tvDetail));

        when(tickerRepository.save(any(Ticker.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ModelMapper modelMapper = new ModelMapper();
        AssetSummaryService service = new AssetSummaryService(
                tickerRepository, quoteService, brapiClient, marketTickerProvider, modelMapper);

        AssetConsolidation position = new AssetConsolidation("BPAC11", new BigDecimal("800.00"), new BigDecimal("10"));
        List<AssetSummaryResponse> summaries = service.createAssetSummaries(List.of(position));

        assertEquals(1, summaries.size());
        assertNotNull(summaries.get(0).getTickerFields());
        assertEquals("EQUITIES", summaries.get(0).getTickerFields().getCategory());
    }

    @Test
    void loadsAllQuotesAndTickersWithOneBatchCallEach() {
        TickerRepository tickerRepository = mock(TickerRepository.class);
        QuoteService quoteService = mock(QuoteService.class);
        BrapiClient brapiClient = mock(BrapiClient.class);
        List<String> symbols = List.of("PETR4", "VALE3", "ITUB4");
        when(quoteService.findQuotes(symbols)).thenReturn(Map.of(
                "PETR4", Quote.builder().symbol("PETR4").regularMarketPrice(35.0).build(),
                "VALE3", Quote.builder().symbol("VALE3").regularMarketPrice(60.0).build(),
                "ITUB4", Quote.builder().symbol("ITUB4").regularMarketPrice(30.0).build()));
        when(tickerRepository.findAllById(symbols)).thenReturn(symbols.stream().map(symbol -> {
            Ticker ticker = new Ticker();
            ticker.setSymbol(symbol);
            ticker.setCategory(AssetCategory.EQUITIES);
            return ticker;
        }).toList());
        AssetSummaryService service = new AssetSummaryService(
                tickerRepository, quoteService, brapiClient, mock(MarketTickerProvider.class), new ModelMapper());

        List<AssetSummaryResponse> summaries = service.createAssetSummaries(symbols.stream()
                .map(symbol -> new AssetConsolidation(symbol, new BigDecimal("100"), BigDecimal.ONE))
                .toList());

        assertEquals(3, summaries.size());
        assertEquals(0, new BigDecimal("60").compareTo(summaries.get(1).getCurrentValue()));
        verify(quoteService, times(1)).findQuotes(symbols);
        verify(quoteService, never()).findQuote(anyString());
        verify(tickerRepository, times(1)).findAllById(symbols);
        verify(tickerRepository, never()).findById(anyString());
        verifyNoInteractions(brapiClient);
    }
}