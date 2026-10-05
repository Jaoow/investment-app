package dev.jaoow.investmentapp.application.service.recommendation;

import dev.jaoow.investmentapp.application.dto.request.SuggestionRequest;
import dev.jaoow.investmentapp.application.dto.response.ExcludedSuggestionResponse;
import dev.jaoow.investmentapp.application.dto.response.SuggestionItemResponse;
import dev.jaoow.investmentapp.application.dto.response.SuggestionResponse;
import dev.jaoow.investmentapp.application.exception.MarketDataUnavailableException;
import dev.jaoow.investmentapp.application.exception.PortfolioNotFoundException;
import dev.jaoow.investmentapp.application.model.AssetConsolidation;
import dev.jaoow.investmentapp.application.model.MarketQuote;
import dev.jaoow.investmentapp.application.service.MarketQuoteFreshness;
import dev.jaoow.investmentapp.application.service.MarketQuoteProvider;
import dev.jaoow.investmentapp.application.service.consolidation.AssetConsolidationService;
import dev.jaoow.investmentapp.domain.entity.AssetAllocation;
import dev.jaoow.investmentapp.domain.entity.CategoryAllocation;
import dev.jaoow.investmentapp.domain.entity.Portfolio;
import dev.jaoow.investmentapp.domain.entity.PortfolioAssetPreference;
import dev.jaoow.investmentapp.domain.repository.CategoryAllocationRepository;
import dev.jaoow.investmentapp.domain.repository.PortfolioAssetPreferenceRepository;
import dev.jaoow.investmentapp.domain.repository.PortfolioRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SuggestionService {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal MAX_CEILING_DISCOUNT = BigDecimal.valueOf(30);
    private static final BigDecimal MAX_ALLOCATION_GAP = BigDecimal.valueOf(20);

    private final PortfolioRepository portfolioRepository;
    private final CategoryAllocationRepository categoryAllocationRepository;
    private final PortfolioAssetPreferenceRepository preferenceRepository;
    private final AssetConsolidationService assetConsolidationService;
    private final MarketQuoteProvider marketQuoteProvider;
    private final MarketQuoteFreshness marketQuoteFreshness;
    private final RecommendationWeights recommendationWeights;
    private final RecommendationAllocator recommendationAllocator;

    public SuggestionService(
            PortfolioRepository portfolioRepository,
            CategoryAllocationRepository categoryAllocationRepository,
            PortfolioAssetPreferenceRepository preferenceRepository,
            AssetConsolidationService assetConsolidationService,
            MarketQuoteProvider marketQuoteProvider,
            MarketQuoteFreshness marketQuoteFreshness,
            RecommendationWeights recommendationWeights,
            RecommendationAllocator recommendationAllocator) {
        this.portfolioRepository = portfolioRepository;
        this.categoryAllocationRepository = categoryAllocationRepository;
        this.preferenceRepository = preferenceRepository;
        this.assetConsolidationService = assetConsolidationService;
        this.marketQuoteProvider = marketQuoteProvider;
        this.marketQuoteFreshness = marketQuoteFreshness;
        this.recommendationWeights = recommendationWeights;
        this.recommendationAllocator = recommendationAllocator;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('USER') and @portfolioSecurity.isOwner(#portfolioId, authentication)")
    public SuggestionResponse suggest(Long portfolioId, SuggestionRequest request) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new PortfolioNotFoundException(portfolioId));

        Map<String, BigDecimal> quantities = assetConsolidationService.processAssetMovements(portfolio).stream()
                .collect(Collectors.toMap(AssetConsolidation::getTickerSymbol, AssetConsolidation::getTotalQuantity));
        Map<String, BigDecimal> targetPercentages = effectiveTargetPercentages(
                categoryAllocationRepository.findByPortfolioId(portfolioId));
        Map<String, PortfolioAssetPreference> preferences = preferenceRepository.findAllByPortfolioId(portfolioId).stream()
                .collect(Collectors.toMap(PortfolioAssetPreference::getTickerSymbol, preference -> preference));

        Set<String> quoteSymbols = new HashSet<>(quantities.keySet());
        targetPercentages.keySet().stream()
                .filter(preferences::containsKey)
                .forEach(quoteSymbols::add);

        Map<String, MarketQuote> quotes = loadFreshQuotes(quoteSymbols, request.getCurrency());
        BigDecimal currentPortfolioValue = quantities.entrySet().stream()
                .map(entry -> entry.getValue().multiply(quotes.get(entry.getKey()).price()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal projectedPortfolioValue = currentPortfolioValue.add(request.getAmount());

        List<ExcludedSuggestionResponse> excluded = new ArrayList<>();
        List<PurchaseCandidate> candidates = buildCandidates(
                targetPercentages, preferences, quantities, quotes, currentPortfolioValue,
                projectedPortfolioValue, excluded);
        List<RecommendationAllocation> allocations = recommendationAllocator.allocate(request.getAmount(), candidates);
        Set<String> allocatedSymbols = allocations.stream()
                .map(allocation -> allocation.candidate().tickerSymbol())
                .collect(Collectors.toCollection(HashSet::new));
        BigDecimal remainingAmount = request.getAmount().subtract(allocations.stream()
                .map(RecommendationAllocation::estimatedValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        for (PurchaseCandidate candidate : candidates) {
            if (!allocatedSymbols.contains(candidate.tickerSymbol())
                    && remainingAmount.compareTo(candidate.price()) < 0
                    && excluded.stream().noneMatch(item -> item.getTickerSymbol().equals(candidate.tickerSymbol()))) {
                excluded.add(new ExcludedSuggestionResponse(
                        candidate.tickerSymbol(),
                        "O aporte/saldo restante não é suficiente para comprar uma unidade inteira."));
            }
        }

        SuggestionResponse response = new SuggestionResponse();
        response.setRequestedAmount(request.getAmount());
        response.setAllocatedAmount(allocations.stream()
                .map(RecommendationAllocation::estimatedValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        response.setRemainingAmount(remainingAmount);
        response.setCurrency(request.getCurrency());
        response.setItems(allocations.stream()
                .map(allocation -> toResponse(allocation, quotes.get(allocation.candidate().tickerSymbol())))
                .toList());
        response.setExcludedAssets(excluded);
        response.setDisclaimer("Sugestão automatizada baseada em regras e dados de mercado; não constitui consultoria financeira nem ordem de compra. Custos e impostos não estão incluídos.");
        return response;
    }

    private Map<String, BigDecimal> effectiveTargetPercentages(List<CategoryAllocation> categoryAllocations) {
        Map<String, BigDecimal> targets = new HashMap<>();
        for (CategoryAllocation categoryAllocation : categoryAllocations) {
            for (AssetAllocation assetAllocation : categoryAllocation.getAssetAllocations()) {
                BigDecimal effectiveTarget = categoryAllocation.getTargetPercentage()
                        .multiply(assetAllocation.getTargetPercentage())
                        .divide(ONE_HUNDRED, 8, RoundingMode.HALF_UP);
                if (effectiveTarget.compareTo(BigDecimal.ZERO) > 0) {
                    targets.put(assetAllocation.getTickerSymbol(), effectiveTarget);
                }
            }
        }
        return targets;
    }

    private Map<String, MarketQuote> loadFreshQuotes(Set<String> symbols, String currency) {
        Map<String, MarketQuote> quotes = new HashMap<>();
        for (String symbol : symbols) {
            MarketQuote quote = marketQuoteFreshness.requireFresh(marketQuoteProvider.getQuote(symbol));
            if (!currency.equals(quote.currency())) {
                throw new MarketDataUnavailableException("Quote currency for " + symbol
                        + " does not match requested currency " + currency + ".");
            }
            quotes.put(symbol, quote);
        }
        return quotes;
    }

    private List<PurchaseCandidate> buildCandidates(
            Map<String, BigDecimal> targets,
            Map<String, PortfolioAssetPreference> preferences,
            Map<String, BigDecimal> quantities,
            Map<String, MarketQuote> quotes,
            BigDecimal currentPortfolioValue,
            BigDecimal projectedPortfolioValue,
            List<ExcludedSuggestionResponse> excluded) {
        List<PurchaseCandidate> candidates = new ArrayList<>();
        targets.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(target -> {
            String ticker = target.getKey();
            PortfolioAssetPreference preference = preferences.get(ticker);
                        if (preference != null && !preference.isContributionEnabled()) {
                                excluded.add(new ExcludedSuggestionResponse(ticker, "Ativo desabilitado para sugestões de aporte."));
                                return;
                        }
            if (preference == null) {
                excluded.add(new ExcludedSuggestionResponse(ticker, "Cadastre um preço teto para este ativo nesta carteira."));
                return;
            }

            MarketQuote quote = quotes.get(ticker);
            BigDecimal ceiling = preference.getPriceCeiling();
            if (quote.price().compareTo(ceiling) > 0) {
                excluded.add(new ExcludedSuggestionResponse(ticker, "Cotação acima do preço teto configurado."));
                return;
            }

            BigDecimal currentValue = quantities.getOrDefault(ticker, BigDecimal.ZERO).multiply(quote.price());
            BigDecimal currentPercentage = currentPortfolioValue.compareTo(BigDecimal.ZERO) == 0
                    ? BigDecimal.ZERO
                    : currentValue.multiply(ONE_HUNDRED).divide(currentPortfolioValue, 8, RoundingMode.HALF_UP);
            BigDecimal targetPercentage = target.getValue();
            BigDecimal targetValue = projectedPortfolioValue.multiply(targetPercentage)
                    .divide(ONE_HUNDRED, 8, RoundingMode.HALF_UP);
            BigDecimal headroom = targetValue.subtract(currentValue).max(BigDecimal.ZERO);
            BigDecimal allocationGap = targetPercentage.subtract(currentPercentage).max(BigDecimal.ZERO);
            BigDecimal ceilingDiscount = ceiling.subtract(quote.price())
                    .divide(ceiling, 8, RoundingMode.HALF_UP)
                    .multiply(ONE_HUNDRED);
            BigDecimal ceilingSignal = clamp(ceilingDiscount.divide(MAX_CEILING_DISCOUNT, 8, RoundingMode.HALF_UP));
            BigDecimal allocationSignal = clamp(allocationGap.divide(MAX_ALLOCATION_GAP, 8, RoundingMode.HALF_UP));
            BigDecimal dailyDropSignal = quote.dailyChangePercent() == null
                    ? null
                    : clamp(quote.dailyChangePercent().negate().max(BigDecimal.ZERO)
                    .divide(BigDecimal.TEN, 8, RoundingMode.HALF_UP));
            BigDecimal score = recommendationWeights.score(ceilingSignal, dailyDropSignal, allocationSignal);

            if (headroom.compareTo(quote.price()) < 0) {
                excluded.add(new ExcludedSuggestionResponse(ticker, "O saldo do alvo é menor que o preço de uma unidade."));
                return;
            }
            if (score.compareTo(BigDecimal.ZERO) == 0) {
                excluded.add(new ExcludedSuggestionResponse(ticker, "Nenhum dos sinais configurados prioriza este ativo."));
                return;
            }

            BigDecimal currentPctPoints = currentPercentage.setScale(2, RoundingMode.HALF_UP);
            BigDecimal targetPctPoints = targetPercentage.setScale(2, RoundingMode.HALF_UP);
            List<String> reasons = new ArrayList<>();
            reasons.add("Cotação " + ceilingDiscount.setScale(2, RoundingMode.HALF_UP)
                    + "% abaixo do preço teto.");
            if (quote.dailyChangePercent() != null && quote.dailyChangePercent().compareTo(BigDecimal.ZERO) < 0) {
                reasons.add("Queda diária de " + quote.dailyChangePercent().abs().setScale(2, RoundingMode.HALF_UP) + "%.");
            }
            if (allocationGap.compareTo(BigDecimal.ZERO) > 0) {
                reasons.add("Alocação " + allocationGap.setScale(2, RoundingMode.HALF_UP)
                        + " p.p. abaixo do alvo.");
            }

            candidates.add(new PurchaseCandidate(
                    ticker,
                    quote.price(),
                    ceiling,
                    score,
                    currentPctPoints,
                    targetPctPoints,
                    ceilingDiscount.setScale(2, RoundingMode.HALF_UP),
                    quote.dailyChangePercent(),
                    headroom,
                    reasons
            ));
        });
        return candidates;
    }

    private BigDecimal clamp(BigDecimal value) {
        return value.max(BigDecimal.ZERO).min(BigDecimal.ONE);
    }

    private SuggestionItemResponse toResponse(RecommendationAllocation allocation, MarketQuote quote) {
        PurchaseCandidate candidate = allocation.candidate();
        SuggestionItemResponse item = new SuggestionItemResponse();
        item.setTickerSymbol(candidate.tickerSymbol());
        item.setQuantity(allocation.quantity());
        item.setUnitPrice(candidate.price());
        item.setEstimatedValue(allocation.estimatedValue());
        item.setScore(candidate.score());
        item.setCeilingPrice(candidate.ceiling());
        item.setCeilingDistancePercentage(candidate.ceilingDistancePercentage());
        item.setDailyChangePercent(candidate.dailyChangePercent());
        item.setCurrentPercentage(candidate.currentPercentage());
        item.setTargetPercentage(candidate.targetPercentage());
        item.setReasons(candidate.reasons());
        item.setQuoteProvider(quote.provider());
        item.setQuoteObservedAt(quote.observedAt());
        return item;
    }
}
