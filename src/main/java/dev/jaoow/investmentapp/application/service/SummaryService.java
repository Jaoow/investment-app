package dev.jaoow.investmentapp.application.service;

import dev.jaoow.investmentapp.application.dto.response.summary.AssetSummaryResponse;
import dev.jaoow.investmentapp.application.dto.response.BrapiQuoteDto;
import dev.jaoow.investmentapp.application.dto.response.summary.PortfolioSummaryResponse;
import dev.jaoow.investmentapp.domain.entity.AssetMovement;
import dev.jaoow.investmentapp.domain.model.MovementType;
import dev.jaoow.investmentapp.domain.entity.Portfolio;
import dev.jaoow.investmentapp.domain.entity.Ticker;
import dev.jaoow.investmentapp.domain.repository.PortfolioRepository;
import dev.jaoow.investmentapp.domain.repository.TickerRepository;
import dev.jaoow.investmentapp.infrastructure.client.BrapiClient;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Service
public class SummaryService {
    private final PortfolioRepository portfolioRepository;
    private final TickerRepository tickerRepository;
    private final BrapiClient brapiClient;
    private final ModelMapper modelMapper;

    public SummaryService(PortfolioRepository portfolioRepository, TickerRepository tickerRepository, BrapiClient brapiClient, ModelMapper modelMapper) {
        this.portfolioRepository = portfolioRepository;
        this.tickerRepository = tickerRepository;
        this.brapiClient = brapiClient;
        this.modelMapper = modelMapper;
    }

    // Get portfolio summary with optional category filtering
    public PortfolioSummaryResponse getPortfolioSummary(Long portfolioId, String category) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new RuntimeException("Portfolio not found"));

        BigDecimal totalInvested = BigDecimal.ZERO;
        BigDecimal currentValue = BigDecimal.ZERO;
        List<AssetSummaryResponse> assetSummaries = new ArrayList<>();

        Map<String, BigDecimal> assetInvestments = new HashMap<>();
        Map<String, BigDecimal> assetQuantities = new HashMap<>();

        // Filter and process asset movements based on the category, if provided
        for (AssetMovement movement : portfolio.getAssetMovements()) {
            Ticker ticker = tickerRepository.findById(movement.getTickerSymbol()).orElse(null);

            if (ticker == null) continue;

            // Apply category filter if provided
            if (category != null && !ticker.getCategory().name().equalsIgnoreCase(category)) {
                continue;
            }

            BigDecimal movementTotal = movement.getQuantity().multiply(movement.getPrice());

            if (movement.getType() == MovementType.BUY) {
                totalInvested = totalInvested.add(movementTotal);
                assetInvestments.put(movement.getTickerSymbol(),
                        assetInvestments.getOrDefault(movement.getTickerSymbol(), BigDecimal.ZERO).add(movementTotal));
                assetQuantities.put(movement.getTickerSymbol(),
                        assetQuantities.getOrDefault(movement.getTickerSymbol(), BigDecimal.ZERO).add(movement.getQuantity()));
            } else if (movement.getType() == MovementType.SELL) {
                totalInvested = totalInvested.subtract(movementTotal);
                assetInvestments.put(movement.getTickerSymbol(),
                        assetInvestments.getOrDefault(movement.getTickerSymbol(), BigDecimal.ZERO).subtract(movementTotal));
                assetQuantities.put(movement.getTickerSymbol(),
                        assetQuantities.getOrDefault(movement.getTickerSymbol(), BigDecimal.ZERO).subtract(movement.getQuantity()));
            }
        }

        // Generate asset summaries from the filtered asset movements
        for (String tickerSymbol : assetInvestments.keySet()) {
            BigDecimal investedAmount = assetInvestments.get(tickerSymbol);
            BigDecimal totalQuantity = assetQuantities.get(tickerSymbol);

            Optional<BrapiQuoteDto> currentQuote = brapiClient.getQuote(tickerSymbol, null, null, null, null);
            BigDecimal currentAssetValue = totalQuantity.multiply(currentQuote.map(BrapiQuoteDto::getRegularMarketPrice).orElse(BigDecimal.ZERO));
            currentValue = currentValue.add(currentAssetValue);

            AssetSummaryResponse assetSummary = getAssetSummaryResponse(tickerSymbol, currentAssetValue, investedAmount);

            // Map the current quote to the asset summary
            currentQuote.ifPresent(brapiQuoteDto -> {
                modelMapper.map(brapiQuoteDto, assetSummary);
            });

            // Fetch and set additional ticker information from the TickerRepository
            tickerRepository.findById(tickerSymbol).ifPresent(ticker -> {
                modelMapper.map(ticker, assetSummary);
            });

            assetSummaries.add(assetSummary);
        }

        BigDecimal overallProfitOrLoss = currentValue.subtract(totalInvested);
        BigDecimal overallPercentageChange = totalInvested.equals(BigDecimal.ZERO) ? BigDecimal.ZERO : overallProfitOrLoss.divide(totalInvested, 2, BigDecimal.ROUND_HALF_UP).multiply(BigDecimal.valueOf(100));

        PortfolioSummaryResponse summary = new PortfolioSummaryResponse();
        summary.setPortfolioId(portfolioId);
        summary.setTotalInvested(totalInvested);
        summary.setCurrentValue(currentValue);
        summary.setProfitOrLoss(overallProfitOrLoss);
        summary.setPercentageChange(overallPercentageChange);
        summary.setAssetSummaries(assetSummaries);

        return summary;
    }

    private static AssetSummaryResponse getAssetSummaryResponse(String tickerSymbol, BigDecimal currentAssetValue, BigDecimal investedAmount) {
        BigDecimal profitOrLoss = currentAssetValue.subtract(investedAmount);
        BigDecimal percentageChange = investedAmount.equals(BigDecimal.ZERO) ? BigDecimal.ZERO : profitOrLoss.divide(investedAmount, 2, BigDecimal.ROUND_HALF_UP).multiply(BigDecimal.valueOf(100));

        AssetSummaryResponse assetSummary = new AssetSummaryResponse();
        assetSummary.setTickerSymbol(tickerSymbol);
        assetSummary.setTotalInvested(investedAmount);
        assetSummary.setCurrentValue(currentAssetValue);
        assetSummary.setProfitOrLoss(profitOrLoss);
        assetSummary.setPercentageChange(percentageChange);
        return assetSummary;
    }
}
