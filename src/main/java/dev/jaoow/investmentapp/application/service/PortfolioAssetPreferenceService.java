package dev.jaoow.investmentapp.application.service;

import dev.jaoow.investmentapp.application.dto.request.PortfolioAssetPreferenceRequest;
import dev.jaoow.investmentapp.application.dto.response.PortfolioAssetPreferenceResponse;
import dev.jaoow.investmentapp.application.dto.response.PriceCeilingHistoryResponse;
import dev.jaoow.investmentapp.application.dto.response.PortfolioAssetSettingResponse;
import dev.jaoow.investmentapp.application.exception.PortfolioNotFoundException;
import dev.jaoow.investmentapp.application.exception.ResourceNotFoundException;
import dev.jaoow.investmentapp.application.exception.TickerNotFoundException;
import dev.jaoow.investmentapp.domain.entity.Portfolio;
import dev.jaoow.investmentapp.domain.entity.PortfolioAssetPreference;
import dev.jaoow.investmentapp.domain.entity.PriceCeilingHistory;
import dev.jaoow.investmentapp.domain.entity.CategoryAllocation;
import dev.jaoow.investmentapp.domain.repository.PortfolioAssetPreferenceRepository;
import dev.jaoow.investmentapp.domain.repository.PortfolioRepository;
import dev.jaoow.investmentapp.domain.repository.TickerRepository;
import dev.jaoow.investmentapp.domain.repository.PriceCeilingHistoryRepository;
import dev.jaoow.investmentapp.domain.repository.CategoryAllocationRepository;
import dev.jaoow.investmentapp.application.util.TickerSymbol;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PagedModel;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PortfolioAssetPreferenceService {

    private final PortfolioAssetPreferenceRepository preferenceRepository;
    private final PortfolioRepository portfolioRepository;
    private final TickerRepository tickerRepository;
    private final PriceCeilingHistoryRepository historyRepository;
    private final CategoryAllocationRepository allocationRepository;

    public PortfolioAssetPreferenceService(
            PortfolioAssetPreferenceRepository preferenceRepository,
            PortfolioRepository portfolioRepository,
            TickerRepository tickerRepository,
            PriceCeilingHistoryRepository historyRepository,
            CategoryAllocationRepository allocationRepository) {
        this.preferenceRepository = preferenceRepository;
        this.portfolioRepository = portfolioRepository;
        this.tickerRepository = tickerRepository;
        this.historyRepository = historyRepository;
        this.allocationRepository = allocationRepository;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('USER') and @portfolioSecurity.isOwner(#portfolioId, authentication)")
    public PortfolioAssetPreferenceResponse getPreference(Long portfolioId, String tickerSymbol) {
        String symbol = TickerSymbol.normalize(tickerSymbol);
        PortfolioAssetPreference preference = preferenceRepository
                .findByPortfolioIdAndTickerSymbol(portfolioId, symbol)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No price ceiling is configured for " + symbol + " in this portfolio."));
        return toResponse(preference);
    }

    @Transactional
    @PreAuthorize("hasRole('USER') and @portfolioSecurity.isOwner(#portfolioId, authentication)")
    public PortfolioAssetPreferenceResponse setPreference(
            Long portfolioId,
            String tickerSymbol,
            PortfolioAssetPreferenceRequest request) {
        tickerSymbol = TickerSymbol.normalize(tickerSymbol);
        if (!tickerRepository.existsById(tickerSymbol)) {
            throw new TickerNotFoundException(tickerSymbol);
        }

        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new PortfolioNotFoundException(portfolioId));
        PortfolioAssetPreference preference = preferenceRepository
                .findByPortfolioIdAndTickerSymbol(portfolioId, tickerSymbol)
                .orElseGet(PortfolioAssetPreference::new);
        preference.setPortfolio(portfolio);
        preference.setTickerSymbol(tickerSymbol);
        BigDecimal previousPrice = preference.getPriceCeiling();
        if (previousPrice == null || previousPrice.compareTo(request.getPriceCeiling()) != 0) {
            recordHistory(portfolioId, tickerSymbol, previousPrice, request.getPriceCeiling(), "SET");
        }
        preference.setPriceCeiling(request.getPriceCeiling());

        return toResponse(preferenceRepository.save(preference));
    }

    @Transactional
    @PreAuthorize("hasRole('USER') and @portfolioSecurity.isOwner(#portfolioId, authentication)")
    public void deletePreference(Long portfolioId, String tickerSymbol) {
        tickerSymbol = TickerSymbol.normalize(tickerSymbol);
        if (!tickerRepository.existsById(tickerSymbol)) {
            throw new TickerNotFoundException(tickerSymbol);
        }
        PortfolioAssetPreference preference = preferenceRepository
                .findByPortfolioIdAndTickerSymbol(portfolioId, tickerSymbol)
                .orElseThrow(() -> new ResourceNotFoundException("Nenhum preço teto configurado para este ativo."));
        recordHistory(portfolioId, tickerSymbol, preference.getPriceCeiling(), null, "REMOVED");
        preferenceRepository.deleteByPortfolioIdAndTickerSymbol(portfolioId, tickerSymbol);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('USER') and @portfolioSecurity.isOwner(#portfolioId, authentication)")
    public PagedModel<PriceCeilingHistoryResponse> getHistory(Long portfolioId, String tickerSymbol, Pageable pageable) {
        String symbol = TickerSymbol.normalize(tickerSymbol);
        return new PagedModel<>(historyRepository
                .findByPortfolioIdAndTickerSymbolOrderByChangedAtDescIdDesc(
                        portfolioId, symbol, PageRequest.of(pageable.getPageNumber(), Math.min(pageable.getPageSize(), 100)))
                .map(entry -> new PriceCeilingHistoryResponse(
                        entry.getId(), entry.getTickerSymbol(), entry.getPreviousPrice(),
                        entry.getPriceCeiling(), entry.getAction(), entry.getChangedAt())));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('USER') and @portfolioSecurity.isOwner(#portfolioId, authentication)")
    public List<PortfolioAssetSettingResponse> getSettings(Long portfolioId) {
        Map<String, PortfolioAssetPreference> preferences = preferenceRepository.findAllByPortfolioId(portfolioId)
                .stream().collect(Collectors.toMap(PortfolioAssetPreference::getTickerSymbol, Function.identity()));
        Map<String, PortfolioAssetSettingResponse> settings = new TreeMap<>();
        for (CategoryAllocation category : allocationRepository.findByPortfolioId(portfolioId)) {
            for (var asset : category.getAssetAllocations()) {
                PortfolioAssetPreference preference = preferences.get(asset.getTickerSymbol());
                settings.put(asset.getTickerSymbol(), new PortfolioAssetSettingResponse(
                        asset.getTickerSymbol(), category.getCategory(), category.getTargetPercentage(),
                        asset.getTargetPercentage(),
                        category.getTargetPercentage().multiply(asset.getTargetPercentage()).movePointLeft(2),
                        preference == null ? null : preference.getPriceCeiling()));
            }
        }
        Map<String, dev.jaoow.investmentapp.domain.entity.Ticker> tickers = tickerRepository
                .findAllById(preferences.keySet()).stream()
                .collect(Collectors.toMap(dev.jaoow.investmentapp.domain.entity.Ticker::getSymbol, Function.identity()));
        preferences.forEach((symbol, preference) -> {
            if (!settings.containsKey(symbol)) {
                settings.put(symbol, new PortfolioAssetSettingResponse(
                        symbol, tickers.get(symbol).getCategory(), BigDecimal.ZERO, BigDecimal.ZERO,
                        BigDecimal.ZERO, preference.getPriceCeiling()));
            }
        });
        return List.copyOf(settings.values());
    }

    private void recordHistory(Long portfolioId, String symbol, BigDecimal previous, BigDecimal price, String action) {
        PriceCeilingHistory entry = new PriceCeilingHistory();
        entry.setPortfolioId(portfolioId);
        entry.setTickerSymbol(symbol);
        entry.setPreviousPrice(previous);
        entry.setPriceCeiling(price);
        entry.setAction(action);
        entry.setChangedAt(Instant.now());
        historyRepository.save(entry);
    }

    private PortfolioAssetPreferenceResponse toResponse(PortfolioAssetPreference preference) {
        return new PortfolioAssetPreferenceResponse(preference.getTickerSymbol(), preference.getPriceCeiling());
    }
}
