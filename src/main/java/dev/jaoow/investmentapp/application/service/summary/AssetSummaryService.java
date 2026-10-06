package dev.jaoow.investmentapp.application.service.summary;

import dev.jaoow.investmentapp.application.dto.response.BrapiQuoteDto;
import dev.jaoow.investmentapp.application.dto.response.summary.AssetSummaryResponse;
import dev.jaoow.investmentapp.application.exception.MarketDataUnavailableException;
import dev.jaoow.investmentapp.application.model.AssetConsolidation;
import dev.jaoow.investmentapp.application.model.BrapiFields;
import dev.jaoow.investmentapp.application.model.TickerFields;
import dev.jaoow.investmentapp.domain.entity.Ticker;
import dev.jaoow.investmentapp.domain.repository.TickerRepository;
import dev.jaoow.investmentapp.infrastructure.client.BrapiClient;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class AssetSummaryService {

    private final TickerRepository tickerRepository;
    private final BrapiClient brapiClient;
    private final ModelMapper modelMapper;

    public AssetSummaryService(TickerRepository tickerRepository, BrapiClient brapiClient, ModelMapper modelMapper) {
        this.tickerRepository = tickerRepository;
        this.brapiClient = brapiClient;
        this.modelMapper = modelMapper;
    }

    public List<AssetSummaryResponse> createAssetSummaries(List<AssetConsolidation> assetConsolidations) {
        List<AssetSummaryResponse> assetSummaries = new ArrayList<>();

        for (AssetConsolidation consolidation : assetConsolidations) {
            BigDecimal investedAmount = consolidation.getInvestedAmount();
            BigDecimal totalQuantity = consolidation.getTotalQuantity();

            String tickerSymbol = consolidation.getTickerSymbol();
            Optional<BrapiQuoteDto> currentQuote = brapiClient.getQuote(tickerSymbol, null, null, null, null);
                BigDecimal currentPrice = currentQuote.map(BrapiQuoteDto::getRegularMarketPrice)
                    .filter(price -> price != null && price.compareTo(BigDecimal.ZERO) > 0)
                    .orElseThrow(() -> new MarketDataUnavailableException("No valid quote was returned for " + tickerSymbol + "."));

                BigDecimal currentAssetValue = totalQuantity.multiply(currentPrice);

            AssetSummaryResponse assetSummary = createAssetSummary(tickerSymbol, currentAssetValue, investedAmount, totalQuantity);
            Optional<Ticker> foundTicker = tickerRepository.findById(tickerSymbol);

            mapQuoteAndTickerInfo(foundTicker, currentQuote, assetSummary);

            assetSummaries.add(assetSummary);
        }

        return assetSummaries;
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

    private void mapQuoteAndTickerInfo(Optional<Ticker> foundTicker, Optional<BrapiQuoteDto> currentQuote, AssetSummaryResponse assetSummary) {
        currentQuote.ifPresent(quote -> {
            BrapiFields brapiFields = modelMapper.map(quote, BrapiFields.class);
            assetSummary.setBrapiFields(brapiFields);
        });

        foundTicker.ifPresent(ticker -> {
            TickerFields tickerFields = modelMapper.map(ticker, TickerFields.class);
            assetSummary.setTickerFields(tickerFields);
        });
    }


    private BigDecimal calculatePercentageChange(BigDecimal investedAmount, BigDecimal profitOrLoss) {
        return investedAmount.equals(BigDecimal.ZERO) ? BigDecimal.ZERO :
                profitOrLoss.divide(investedAmount, 2, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
    }

    private BigDecimal calculateAveragePrice(BigDecimal investedAmount, BigDecimal totalQuantity) {
        return totalQuantity.equals(BigDecimal.ZERO) ? BigDecimal.ZERO :
                investedAmount.divide(totalQuantity, 2, RoundingMode.HALF_UP);
    }
}
