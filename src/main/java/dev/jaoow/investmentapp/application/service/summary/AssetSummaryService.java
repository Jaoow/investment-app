package dev.jaoow.investmentapp.application.service.summary;

import dev.jaoow.cotatrack.api.requests.quotes.model.Quote;
import dev.jaoow.investmentapp.application.dto.response.BrapiQuoteDto;
import dev.jaoow.investmentapp.application.dto.response.BrapiTickerResolveResultDto;
import dev.jaoow.investmentapp.application.dto.response.summary.AssetSummaryResponse;
import dev.jaoow.investmentapp.application.model.AssetConsolidation;
import dev.jaoow.investmentapp.application.model.BrapiFields;
import dev.jaoow.investmentapp.application.model.TickerFields;
import dev.jaoow.investmentapp.application.model.MarketTicker;
import dev.jaoow.cotatrack.api.classification.AssetClassifier;
import dev.jaoow.investmentapp.application.service.MarketTickerProvider;
import dev.jaoow.investmentapp.application.service.QuoteService;
import dev.jaoow.investmentapp.application.util.SectorTranslator;
import dev.jaoow.investmentapp.domain.entity.Ticker;
import dev.jaoow.investmentapp.domain.model.AssetCategory;
import dev.jaoow.investmentapp.domain.repository.TickerRepository;
import dev.jaoow.investmentapp.infrastructure.client.BrapiClient;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Cria resumos de ativos do portfólio usando cotações do CotaTrack
 * (TradingView → Yahoo Chart → Brapi) com fallback para Brapi TickerResolver
 * e auto-descoberta de categoria e metadados de ativos.
 */
@Slf4j
@Service
public class AssetSummaryService {

    private final TickerRepository tickerRepository;
    private final QuoteService quoteService;
    private final BrapiClient brapiClient;
    private final MarketTickerProvider marketTickerProvider;
    private final ModelMapper modelMapper;

    public AssetSummaryService(
            TickerRepository tickerRepository,
            QuoteService quoteService,
            BrapiClient brapiClient,
            MarketTickerProvider marketTickerProvider,
            ModelMapper modelMapper) {
        this.tickerRepository = tickerRepository;
        this.quoteService = quoteService;
        this.brapiClient = brapiClient;
        this.marketTickerProvider = marketTickerProvider;
        this.modelMapper = modelMapper;
    }

    public List<AssetSummaryResponse> createAssetSummaries(List<AssetConsolidation> assetConsolidations) {
        List<AssetSummaryResponse> assetSummaries = new ArrayList<>();

        for (AssetConsolidation consolidation : assetConsolidations) {
            BigDecimal investedAmount = consolidation.getInvestedAmount();
            BigDecimal totalQuantity = consolidation.getTotalQuantity();
            String tickerSymbol = consolidation.getTickerSymbol();

            // 1. Fetch quote via CotaTrack multi-provider orchestrator
            Quote currentQuote = fetchQuote(tickerSymbol);
            BigDecimal currentPrice = extractPrice(currentQuote);

            // 2. Fallback: If quote missing or price <= 0, try Brapi ticker resolver and Brapi direct quote
            if (currentPrice == null || currentPrice.compareTo(BigDecimal.ZERO) <= 0) {
                try {
                    List<BrapiTickerResolveResultDto> resolveResults = brapiClient
                            .resolveTickers(List.of(tickerSymbol));
                    if (resolveResults != null && !resolveResults.isEmpty()) {
                        BrapiTickerResolveResultDto result = resolveResults.getFirst();
                        if (result.isChanged() && result.getSymbol() != null) {
                            String newTicker = result.getSymbol();
                            currentQuote = fetchQuote(newTicker);
                            currentPrice = extractPrice(currentQuote);

                            if (currentPrice == null || currentPrice.compareTo(BigDecimal.ZERO) <= 0) {
                                Optional<BrapiQuoteDto> brapiQuote = brapiClient.getQuote(newTicker, null, null, null, null);
                                if (brapiQuote.isPresent() && brapiQuote.get().getRegularMarketPrice() != null) {
                                    currentPrice = brapiQuote.get().getRegularMarketPrice();
                                    if (currentQuote == null) {
                                        currentQuote = mapBrapiDtoToQuote(brapiQuote.get());
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception ex) {
                    log.warn("Ticker resolution via Brapi failed for {}: {}", tickerSymbol, ex.getMessage());
                }

                // If still no quote, try direct Brapi quote as final fallback
                if (currentPrice == null || currentPrice.compareTo(BigDecimal.ZERO) <= 0) {
                    try {
                        Optional<BrapiQuoteDto> brapiQuote = brapiClient.getQuote(tickerSymbol, null, null, null, null);
                        if (brapiQuote.isPresent() && brapiQuote.get().getRegularMarketPrice() != null) {
                            currentPrice = brapiQuote.get().getRegularMarketPrice();
                            if (currentQuote == null) {
                                currentQuote = mapBrapiDtoToQuote(brapiQuote.get());
                            }
                        }
                    } catch (Exception ex) {
                        log.debug("Direct Brapi fallback failed for {}: {}", tickerSymbol, ex.getMessage());
                    }
                }
            }

            if (currentPrice == null) {
                currentPrice = BigDecimal.ZERO;
            }

            BigDecimal currentAssetValue = totalQuantity.multiply(currentPrice);
            AssetSummaryResponse assetSummary = createAssetSummary(
                    tickerSymbol, currentAssetValue, investedAmount, totalQuantity);

            Optional<Ticker> foundTicker = tickerRepository.findById(tickerSymbol);
            if (foundTicker.isEmpty()) {
                foundTicker = autoDiscoverTicker(tickerSymbol, currentQuote);
            }
            mapQuoteAndTickerInfo(foundTicker, currentQuote, assetSummary);

            assetSummaries.add(assetSummary);
        }

        return assetSummaries;
    }

    public List<AssetSummaryResponse> createAssetSummariesWithoutQuotes(List<AssetConsolidation> assetConsolidations) {
        List<AssetSummaryResponse> assetSummaries = new ArrayList<>();
        for (AssetConsolidation consolidation : assetConsolidations) {
            BigDecimal investedAmount = consolidation.getInvestedAmount();
            BigDecimal totalQuantity = consolidation.getTotalQuantity();
            AssetSummaryResponse assetSummary = createAssetSummary(
                    consolidation.getTickerSymbol(), BigDecimal.ZERO, investedAmount, totalQuantity);
            mapQuoteAndTickerInfo(
                    tickerRepository.findById(consolidation.getTickerSymbol()), null, assetSummary);
            assetSummaries.add(assetSummary);
        }
        return assetSummaries;
    }

    private Quote fetchQuote(String tickerSymbol) {
        return quoteService.findQuote(tickerSymbol).orElse(null);
    }

    private BigDecimal extractPrice(Quote quote) {
        if (quote == null || quote.getRegularMarketPrice() == null) return BigDecimal.ZERO;
        BigDecimal price = BigDecimal.valueOf(quote.getRegularMarketPrice());
        return price.compareTo(BigDecimal.ZERO) > 0 ? price : BigDecimal.ZERO;
    }

    private AssetSummaryResponse createAssetSummary(String tickerSymbol, BigDecimal currentAssetValue,
            BigDecimal investedAmount, BigDecimal totalQuantity) {
        BigDecimal profitOrLoss = currentAssetValue.subtract(investedAmount);
        BigDecimal percentageChange = calculatePercentageChange(investedAmount, profitOrLoss);
        BigDecimal averagePrice = calculateAveragePrice(investedAmount, totalQuantity);

        return AssetSummaryResponse.builder()
                .tickerSymbol(tickerSymbol)
                .quantity(totalQuantity)
                .totalInvested(investedAmount)
                .currentValue(currentAssetValue)
                .profitOrLoss(profitOrLoss)
                .percentageChange(percentageChange)
                .averagePrice(averagePrice)
                .build();
    }

    private void mapQuoteAndTickerInfo(Optional<Ticker> foundTicker, Quote currentQuote,
            AssetSummaryResponse assetSummary) {
        if (currentQuote != null) {
            BrapiFields fields = mapQuoteToFields(currentQuote);
            assetSummary.setBrapiFields(fields);
        }

        if (foundTicker.isPresent()) {
            Ticker ticker = foundTicker.get();
            boolean modified = false;
            if ((ticker.getLogoUrl() == null || ticker.getLogoUrl().isBlank()) && currentQuote != null && currentQuote.getLogoUrl() != null) {
                ticker.setLogoUrl(currentQuote.getLogoUrl());
                modified = true;
            }
            if ((ticker.getName() == null || ticker.getName().equalsIgnoreCase(ticker.getSymbol())) && currentQuote != null && currentQuote.getLongName() != null && !currentQuote.getLongName().isBlank()) {
                ticker.setName(currentQuote.getLongName());
                modified = true;
            }
            if (modified) {
                try {
                    ticker = tickerRepository.save(ticker);
                } catch (Exception ex) {
                    log.debug("Could not backfill ticker metadata for {}: {}", ticker.getSymbol(), ex.getMessage());
                }
            }
            TickerFields tickerFields = new TickerFields();
            tickerFields.setName(ticker.getName());
            tickerFields.setShortName(currentQuote != null && currentQuote.getShortName() != null ? currentQuote.getShortName() : ticker.getSymbol());
            tickerFields.setLongName(currentQuote != null && currentQuote.getLongName() != null ? currentQuote.getLongName() : ticker.getName());
            tickerFields.setLogoUrl(ticker.getLogoUrl());
            tickerFields.setCategory(ticker.getCategory() != null ? ticker.getCategory().name() : null);
            tickerFields.setSector(SectorTranslator.translate(ticker.getSector()));
            tickerFields.setSubSector(ticker.getSubSector());
            assetSummary.setTickerFields(tickerFields);
        } else if (currentQuote != null) {
            TickerFields tickerFields = new TickerFields();
            String fullName = (currentQuote.getLongName() != null && !currentQuote.getLongName().isBlank() && !currentQuote.getLongName().equalsIgnoreCase(currentQuote.getSymbol()))
                    ? currentQuote.getLongName()
                    : (currentQuote.getShortName() != null ? currentQuote.getShortName() : assetSummary.getTickerSymbol());
            tickerFields.setName(fullName);
            tickerFields.setShortName(currentQuote.getShortName());
            tickerFields.setLongName(currentQuote.getLongName());
            tickerFields.setLogoUrl(currentQuote.getLogoUrl());
            tickerFields.setCategory(currentQuote.getCategory());
            tickerFields.setSector(SectorTranslator.translate(currentQuote.getSector()));
            assetSummary.setTickerFields(tickerFields);
        }
    }

    private BrapiFields mapQuoteToFields(Quote quote) {
        BrapiFields fields = new BrapiFields();
        fields.setShortName(quote.getShortName());
        fields.setLongName(quote.getLongName());
        fields.setRegularMarketPrice(
                quote.getRegularMarketPrice() != null ? BigDecimal.valueOf(quote.getRegularMarketPrice()) : null);
        fields.setRegularMarketChange(
                quote.getRegularMarketChange() != null ? BigDecimal.valueOf(quote.getRegularMarketChange()) : null);
        fields.setRegularMarketChangePercent(
                quote.getRegularMarketChangePercent() != null ? BigDecimal.valueOf(quote.getRegularMarketChangePercent()) : null);
        fields.setRegularMarketDayHigh(
                quote.getRegularMarketDayHigh() != null ? BigDecimal.valueOf(quote.getRegularMarketDayHigh()) : null);
        fields.setRegularMarketDayLow(
                quote.getRegularMarketDayLow() != null ? BigDecimal.valueOf(quote.getRegularMarketDayLow()) : null);
        fields.setRegularMarketVolume(
                quote.getRegularMarketVolume() != null ? BigDecimal.valueOf(quote.getRegularMarketVolume()) : null);
        fields.setRegularMarketOpen(
                quote.getRegularMarketOpen() != null ? BigDecimal.valueOf(quote.getRegularMarketOpen()) : null);
        fields.setLogourl(quote.getLogoUrl());
        return fields;
    }

    private BigDecimal calculatePercentageChange(BigDecimal investedAmount, BigDecimal profitOrLoss) {
        return investedAmount.equals(BigDecimal.ZERO) ? BigDecimal.ZERO
                : profitOrLoss.divide(investedAmount, 2, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
    }

    private BigDecimal calculateAveragePrice(BigDecimal investedAmount, BigDecimal totalQuantity) {
        return totalQuantity.equals(BigDecimal.ZERO) ? BigDecimal.ZERO
                : investedAmount.divide(totalQuantity, 2, RoundingMode.HALF_UP);
    }

    private Quote mapBrapiDtoToQuote(BrapiQuoteDto dto) {
        if (dto == null) return null;
        return Quote.builder()
                .symbol(dto.getSymbol())
                .shortName(dto.getShortName())
                .longName(dto.getLongName())
                .regularMarketPrice(dto.getRegularMarketPrice() != null ? dto.getRegularMarketPrice().doubleValue() : null)
                .regularMarketChange(dto.getRegularMarketChange() != null ? dto.getRegularMarketChange().doubleValue() : null)
                .regularMarketChangePercent(dto.getRegularMarketChangePercent() != null ? dto.getRegularMarketChangePercent().doubleValue() : null)
                .regularMarketDayHigh(dto.getRegularMarketDayHigh() != null ? dto.getRegularMarketDayHigh().doubleValue() : null)
                .regularMarketDayLow(dto.getRegularMarketDayLow() != null ? dto.getRegularMarketDayLow().doubleValue() : null)
                .regularMarketVolume(dto.getRegularMarketVolume() != null ? dto.getRegularMarketVolume().longValue() : null)
                .regularMarketOpen(dto.getRegularMarketOpen() != null ? dto.getRegularMarketOpen().doubleValue() : null)
                .logoUrl(dto.getLogourl())
                .build();
    }

    private Optional<Ticker> autoDiscoverTicker(String tickerSymbol, Quote currentQuote) {
        try {
            Ticker ticker = new Ticker();
            ticker.setSymbol(tickerSymbol);

            String name = currentQuote != null ? currentQuote.getShortName() : null;
            String logoUrl = currentQuote != null ? currentQuote.getLogoUrl() : null;
            String description = currentQuote != null ? currentQuote.getLongName() : null;
            String sector = currentQuote != null ? currentQuote.getSector() : null;
            String subSector = null;
            String industry = currentQuote != null ? currentQuote.getIndustry() : null;
            String type = currentQuote != null ? currentQuote.getType() : null;
            String subType = currentQuote != null ? currentQuote.getSubType() : null;
            String categoryStr = currentQuote != null ? currentQuote.getCategory() : null;

            if (marketTickerProvider != null && (sector == null || industry == null)) {
                try {
                    List<MarketTicker> details = marketTickerProvider.searchTickers(tickerSymbol, null);
                    if (details != null && !details.isEmpty()) {
                        MarketTicker match = details.stream()
                                .filter(t -> t.getSymbol().equalsIgnoreCase(tickerSymbol))
                                .findFirst()
                                .orElse(details.get(0));
                        if (name == null || name.isBlank()) name = match.getName();
                        if (logoUrl == null) logoUrl = match.getLogoUrl();
                        if (description == null) description = match.getLongName();
                        if (sector == null) sector = match.getSector();
                        if (subSector == null) subSector = match.getSubSector();
                        if (industry == null) industry = match.getIndustry();
                        if (type == null) type = match.getType();
                        if (subType == null) subType = match.getSubType();
                        if (categoryStr == null) categoryStr = match.getCategory();
                    }
                } catch (Exception ex) {
                    log.debug("Provider search failed during auto-discovery for {}: {}", tickerSymbol, ex.getMessage());
                }
            }

            AssetCategory category = null;
            if (categoryStr != null) {
                try {
                    category = AssetCategory.valueOf(categoryStr.toUpperCase());
                } catch (IllegalArgumentException ignored) {}
            }
            if (category == null) {
                String catName = AssetClassifier.classify(tickerSymbol, type, subType, sector, industry, description).getCategoryName();
                try {
                    category = AssetCategory.valueOf(catName);
                } catch (IllegalArgumentException | NullPointerException e) {
                    category = AssetCategory.EQUITIES;
                }
            }

            ticker.setCategory(category);
            String fullName = (description != null && !description.isBlank() && !description.equalsIgnoreCase(tickerSymbol))
                    ? description
                    : ((name != null && !name.isBlank()) ? name : tickerSymbol);
            ticker.setName(fullName);
            ticker.setLogoUrl(logoUrl);
            ticker.setSector(sector);
            ticker.setSubSector(subSector);

            ticker = tickerRepository.save(ticker);
            log.info("Auto-registered ticker {} with category {}", tickerSymbol, category);
            return Optional.of(ticker);
        } catch (Exception ex) {
            log.warn("Failed to auto-discover ticker {}: {}", tickerSymbol, ex.getMessage());
            return Optional.empty();
        }
    }
}
