package dev.jaoow.investmentapp.application.service;

import dev.jaoow.investmentapp.application.dto.request.RebalanceSaleRequest;
import dev.jaoow.investmentapp.application.dto.response.ExcludedSuggestionResponse;
import dev.jaoow.investmentapp.application.dto.response.RebalanceRecommendationResponse;
import dev.jaoow.investmentapp.application.dto.response.RebalanceSalePlanResponse;
import dev.jaoow.investmentapp.application.exception.PortfolioNotFoundException;
import dev.jaoow.investmentapp.application.model.AssetConsolidation;
import dev.jaoow.investmentapp.application.service.consolidation.AssetConsolidationService;
import dev.jaoow.investmentapp.domain.entity.*;
import dev.jaoow.investmentapp.domain.model.AssetCategory;
import dev.jaoow.investmentapp.domain.repository.CategoryAllocationRepository;
import dev.jaoow.investmentapp.domain.repository.PortfolioRepository;
import dev.jaoow.investmentapp.domain.repository.TickerRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RebalanceService {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private static final String SALE_DISCLAIMER = "Simulação baseada nas cotações atuais e nas metas definidas. Não constitui recomendação de investimento nem ordem de venda; custos, impostos sobre ganho de capital e liquidez não estão incluídos.";

    private final PortfolioRepository portfolioRepository;
    private final CategoryAllocationRepository categoryAllocationRepository;
    private final AssetConsolidationService assetConsolidationService;
    private final MarketQuoteLoader marketQuoteLoader;
    private final TickerRepository tickerRepository;

    public RebalanceService(PortfolioRepository portfolioRepository,
                            CategoryAllocationRepository categoryAllocationRepository,
                            AssetConsolidationService assetConsolidationService,
                            MarketQuoteLoader marketQuoteLoader,
                            TickerRepository tickerRepository) {
        this.portfolioRepository = portfolioRepository;
        this.categoryAllocationRepository = categoryAllocationRepository;
        this.assetConsolidationService = assetConsolidationService;
        this.marketQuoteLoader = marketQuoteLoader;
        this.tickerRepository = tickerRepository;
    }

    @PreAuthorize("hasRole('USER') and @portfolioSecurity.isOwner(#portfolioId, authentication)")
    @Transactional(readOnly = true)
    public List<RebalanceRecommendationResponse> generateRebalanceRecommendations(Long portfolioId, String categoryFilter) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new PortfolioNotFoundException(portfolioId));
        List<CategoryAllocation> categoryAllocations = categoryAllocationRepository.findByPortfolioId(portfolioId);
        Map<String, BigDecimal> quantities = assetConsolidationService.processAssetMovements(portfolio).stream()
                .collect(Collectors.toMap(AssetConsolidation::getTickerSymbol, AssetConsolidation::getTotalQuantity));

        Set<String> tickers = new HashSet<>(categoryAllocations.stream()
                .flatMap(category -> category.getAssetAllocations().stream())
                .map(AssetAllocation::getTickerSymbol)
                .collect(Collectors.toSet()));
        tickers.addAll(quantities.keySet());

        Map<String, BigDecimal> prices = loadPrices(tickers);

        BigDecimal portfolioValue = quantities.entrySet().stream()
                .map(entry -> entry.getValue().multiply(prices.get(entry.getKey())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (portfolioValue.compareTo(BigDecimal.ZERO) == 0) {
            return List.of();
        }

        return categoryAllocations.stream()
                .filter(allocation -> categoryFilter == null
                        || allocation.getCategory().name().equalsIgnoreCase(categoryFilter))
                .flatMap(allocation -> allocation.getAssetAllocations().stream()
                        .map(assetAllocation -> buildRecommendation(allocation, assetAllocation, quantities, prices, portfolioValue)))
                .filter(recommendation -> recommendation != null)
                .toList();
    }

    private RebalanceRecommendationResponse buildRecommendation(
            CategoryAllocation categoryAllocation,
            AssetAllocation assetAllocation,
            Map<String, BigDecimal> quantities,
            Map<String, BigDecimal> prices,
            BigDecimal portfolioValue) {
        String ticker = assetAllocation.getTickerSymbol();
        BigDecimal price = prices.get(ticker);
        BigDecimal currentValue = quantities.getOrDefault(ticker, BigDecimal.ZERO).multiply(price);
        BigDecimal targetPercentage = categoryAllocation.getTargetPercentage()
                .multiply(assetAllocation.getTargetPercentage())
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal targetValue = portfolioValue.multiply(targetPercentage)
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal gap = targetValue.subtract(currentValue);
        if (gap.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        BigDecimal quantityToBuy = gap.divideToIntegralValue(price).setScale(0, RoundingMode.DOWN);
        if (quantityToBuy.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        RebalanceRecommendationResponse response = new RebalanceRecommendationResponse();
        response.setTickerSymbol(ticker);
        response.setAction("BUY");
        response.setQuantity(quantityToBuy);
        response.setCurrentPercentage(currentValue.multiply(BigDecimal.valueOf(100))
                .divide(portfolioValue, 4, RoundingMode.HALF_UP));
        response.setTargetPercentage(targetPercentage);
        return response;
    }

    @PreAuthorize("hasRole('USER') and @portfolioSecurity.isOwner(#portfolioId, authentication)")
    @Transactional(readOnly = true)
    public RebalanceSalePlanResponse planSales(Long portfolioId, RebalanceSaleRequest request) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new PortfolioNotFoundException(portfolioId));
        List<CategoryAllocation> categoryAllocations = categoryAllocationRepository.findByPortfolioId(portfolioId);
        Map<String, BigDecimal> targets = effectiveTargets(categoryAllocations);
        List<AssetConsolidation> consolidations = assetConsolidationService.processAssetMovements(portfolio);
        Map<String, BigDecimal> prices = loadPrices(consolidations.stream()
                .map(AssetConsolidation::getTickerSymbol)
                .collect(Collectors.toSet()));
        Map<String, AssetCategory> categories = resolveCategories(categoryAllocations, prices.keySet());

        List<HeldAsset> assets = consolidations.stream()
                .map(consolidation -> HeldAsset.of(consolidation, prices.get(consolidation.getTickerSymbol()),
                        targets.getOrDefault(consolidation.getTickerSymbol(), BigDecimal.ZERO),
                        categories.getOrDefault(consolidation.getTickerSymbol(), AssetCategory.OTHERS)))
                .filter(asset -> asset.price().signum() > 0)
                .sorted(Comparator.comparing(HeldAsset::ticker))
                .toList();
        BigDecimal total = assets.stream().map(HeldAsset::value).reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, BigDecimal> projected = new LinkedHashMap<>();
        assets.forEach(asset -> projected.put(asset.ticker(), asset.value()));
        List<RebalanceSalePlanResponse.Sale> sales = new ArrayList<>();
        List<ExcludedSuggestionResponse> excluded = new ArrayList<>();
        BigDecimal maxWithdrawalAmount = BigDecimal.ZERO;
        if (total.signum() > 0) {
            if (request.mode() == RebalanceSaleRequest.Mode.REALLOCATION) {
                planReallocation(assets, total, projected, sales, excluded);
            } else {
                planProfitTaking(assets, total, request.withdrawalAmount(), projected, sales, excluded);
                maxWithdrawalAmount = request.withdrawalAmount() == null
                        ? sumEstimatedValues(sales)
                        : maxWithdrawal(assets, total);
            }
        }
        boolean configurationRequired = targets.values().stream().noneMatch(target -> target.signum() > 0);
        return buildSalePlan(request.mode(), assets, total, projected, sales, excluded,
                money(maxWithdrawalAmount), configurationRequired);
    }

    private BigDecimal maxWithdrawal(List<HeldAsset> assets, BigDecimal total) {
        List<RebalanceSalePlanResponse.Sale> sales = new ArrayList<>();
        planProfitTaking(assets, total, null, new HashMap<>(), sales, new ArrayList<>());
        return sumEstimatedValues(sales);
    }

    private static BigDecimal sumEstimatedValues(List<RebalanceSalePlanResponse.Sale> sales) {
        return sales.stream().map(RebalanceSalePlanResponse.Sale::estimatedValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void planReallocation(List<HeldAsset> assets, BigDecimal total, Map<String, BigDecimal> projected,
                                  List<RebalanceSalePlanResponse.Sale> sales, List<ExcludedSuggestionResponse> excluded) {
        for (HeldAsset asset : assets) {
            if (asset.target().signum() <= 0) {
                excluded.add(new ExcludedSuggestionResponse(asset.ticker(), "Sem meta definida; posição preservada."));
                continue;
            }
            BigDecimal excess = asset.value().subtract(targetValue(total, asset.target()));
            if (excess.signum() <= 0) {
                continue;
            }
            BigDecimal quantity = excess.divide(asset.price(), 0, RoundingMode.DOWN)
                    .min(asset.quantity().setScale(0, RoundingMode.DOWN));
            if (quantity.signum() == 0) {
                excluded.add(new ExcludedSuggestionResponse(asset.ticker(), "Desvio menor que o preço de uma unidade."));
                continue;
            }
            BigDecimal drift = percentage(asset.value(), total).subtract(asset.target());
            RebalanceSalePlanResponse.Sale sale = sale(asset, quantity, total, List.of(
                    drift.setScale(1, RoundingMode.HALF_UP) + " p.p. acima da meta",
                    "meta de " + asset.target().setScale(1, RoundingMode.HALF_UP) + "% da carteira"));
            projected.put(asset.ticker(), asset.value().subtract(sale.estimatedValue()));
            sales.add(sale);
        }

        BigDecimal saleAmount = sumEstimatedValues(sales);
        Map<String, BigDecimal> deficits = new LinkedHashMap<>();
        assets.forEach(asset -> deficits.put(asset.ticker(),
                targetValue(total, asset.target()).subtract(asset.value()).max(BigDecimal.ZERO)));
        BigDecimal deficitTotal = deficits.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (deficitTotal.signum() > 0) {
            BigDecimal reinvested = saleAmount.min(deficitTotal);
            deficits.forEach((ticker, deficit) -> projected.merge(ticker,
                    reinvested.multiply(deficit).divide(deficitTotal, 8, RoundingMode.HALF_UP), BigDecimal::add));
        }
        sales.sort(Comparator.comparing((RebalanceSalePlanResponse.Sale sale) ->
                sale.currentPercentage().subtract(sale.targetPercentage())).reversed());
    }

    /** Sells only the gain portion of each position so the remaining value stays at or above the invested capital. */
    private void planProfitTaking(List<HeldAsset> assets, BigDecimal total, BigDecimal withdrawalAmount,
                                  Map<String, BigDecimal> projected, List<RebalanceSalePlanResponse.Sale> sales,
                                  List<ExcludedSuggestionResponse> excluded) {
        List<HeldAsset> candidates = new ArrayList<>();
        for (HeldAsset asset : assets) {
            if (asset.price().compareTo(asset.averagePrice()) <= 0) {
                excluded.add(new ExcludedSuggestionResponse(asset.ticker(), "Sem lucro acumulado na posição."));
            } else {
                candidates.add(asset);
            }
        }
        candidates.sort(Comparator.comparing(HeldAsset::gainPercentage).reversed());

        BigDecimal remaining = withdrawalAmount;
        for (HeldAsset asset : candidates) {
            BigDecimal maxQuantity = asset.unrealizedProfit().divide(asset.price(), 0, RoundingMode.DOWN);
            if (maxQuantity.signum() == 0) {
                excluded.add(new ExcludedSuggestionResponse(asset.ticker(), "Lucro menor que o preço de uma unidade."));
                continue;
            }
            BigDecimal quantity = remaining == null
                    ? maxQuantity
                    : maxQuantity.min(remaining.divide(asset.price(), 0, RoundingMode.DOWN));
            if (quantity.signum() == 0) {
                excluded.add(new ExcludedSuggestionResponse(asset.ticker(), "Fora do limite de saque informado."));
                continue;
            }
            RebalanceSalePlanResponse.Sale sale = sale(asset, quantity, total, List.of(
                    asset.gainPercentage().setScale(1, RoundingMode.HALF_UP) + "% de valorização",
                    asset.quantity().subtract(quantity).stripTrailingZeros().toPlainString() + " de "
                            + asset.quantity().stripTrailingZeros().toPlainString() + " unidades mantidas"));
            if (remaining != null) {
                remaining = remaining.subtract(sale.estimatedValue());
            }
            projected.put(asset.ticker(), asset.value().subtract(sale.estimatedValue()));
            sales.add(sale);
        }
    }

    private RebalanceSalePlanResponse buildSalePlan(RebalanceSaleRequest.Mode mode, List<HeldAsset> assets, BigDecimal total,
                                                    Map<String, BigDecimal> projected,
                                                    List<RebalanceSalePlanResponse.Sale> sales,
                                                    List<ExcludedSuggestionResponse> excluded,
                                                    BigDecimal maxWithdrawalAmount,
                                                    boolean configurationRequired) {
        Map<String, HeldAsset> assetsByTicker = assets.stream()
                .collect(Collectors.toMap(HeldAsset::ticker, asset -> asset));
        BigDecimal saleAmount = money(sumEstimatedValues(sales));
        BigDecimal projectedTotal = projected.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        List<RebalanceSalePlanResponse.CategoryImpact> categoryImpact = assets.stream()
                .map(HeldAsset::category)
                .distinct()
                .sorted()
                .map(category -> new RebalanceSalePlanResponse.CategoryImpact(
                        category,
                        percentage(assets.stream().filter(asset -> asset.category() == category)
                                .map(HeldAsset::value).reduce(BigDecimal.ZERO, BigDecimal::add), total),
                        percentage(assets.stream().filter(asset -> asset.category() == category)
                                .map(asset -> projected.get(asset.ticker())).reduce(BigDecimal.ZERO, BigDecimal::add),
                                projectedTotal)))
                .toList();
        return new RebalanceSalePlanResponse(
                mode,
                configurationRequired,
                saleAmount,
                maxWithdrawalAmount,
                money(sales.stream().map(RebalanceSalePlanResponse.Sale::realizedProfit)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)),
                money(assets.stream().map(asset -> asset.unrealizedProfit().max(BigDecimal.ZERO))
                        .reduce(BigDecimal.ZERO, BigDecimal::add)),
                (int) assets.stream().filter(asset -> asset.unrealizedProfit().signum() > 0).count(),
                money(sales.stream().map(sale -> assetsByTicker.get(sale.ticker()).investedAmount())
                        .reduce(BigDecimal.ZERO, BigDecimal::add)),
                money(projectedTotal.subtract(total.subtract(saleAmount)).max(BigDecimal.ZERO)),
                List.copyOf(sales),
                List.copyOf(excluded),
                categoryImpact,
                SALE_DISCLAIMER);
    }

    private RebalanceSalePlanResponse.Sale sale(HeldAsset asset, BigDecimal quantity, BigDecimal total, List<String> explanation) {
        BigDecimal estimatedValue = money(quantity.multiply(asset.price()));
        return new RebalanceSalePlanResponse.Sale(
                asset.ticker(),
                quantity,
                asset.price(),
                estimatedValue,
                money(quantity.multiply(asset.price().subtract(asset.averagePrice()))),
                percentage(asset.value(), total),
                asset.target().setScale(4, RoundingMode.HALF_UP),
                percentage(asset.value().subtract(estimatedValue), total),
                asset.gainPercentage(),
                explanation);
    }

    private Map<String, BigDecimal> effectiveTargets(List<CategoryAllocation> categoryAllocations) {
        Map<String, BigDecimal> targets = new HashMap<>();
        for (CategoryAllocation categoryAllocation : categoryAllocations) {
            for (AssetAllocation assetAllocation : categoryAllocation.getAssetAllocations()) {
                targets.put(assetAllocation.getTickerSymbol(), categoryAllocation.getTargetPercentage()
                        .multiply(assetAllocation.getTargetPercentage())
                        .divide(ONE_HUNDRED, 8, RoundingMode.HALF_UP));
            }
        }
        return targets;
    }

    private Map<String, AssetCategory> resolveCategories(List<CategoryAllocation> categoryAllocations, Set<String> tickers) {
        Map<String, AssetCategory> categories = new HashMap<>();
        categoryAllocations.forEach(allocation -> allocation.getAssetAllocations()
                .forEach(asset -> categories.put(asset.getTickerSymbol(), allocation.getCategory())));
        Set<String> unresolved = tickers.stream()
                .filter(ticker -> !categories.containsKey(ticker))
                .collect(Collectors.toSet());
        if (!unresolved.isEmpty()) {
            tickerRepository.findAllById(unresolved).stream()
                    .filter(ticker -> ticker.getCategory() != null)
                    .forEach(ticker -> categories.put(ticker.getSymbol(), ticker.getCategory()));
        }
        return categories;
    }

    private Map<String, BigDecimal> loadPrices(Set<String> tickers) {
        Map<String, BigDecimal> prices = new HashMap<>();
        marketQuoteLoader.loadFresh(tickers).forEach((ticker, quote) -> prices.put(ticker, quote.price()));
        return prices;
    }

    private static BigDecimal targetValue(BigDecimal total, BigDecimal targetPercentage) {
        return total.multiply(targetPercentage).divide(ONE_HUNDRED, 8, RoundingMode.HALF_UP);
    }

    private static BigDecimal percentage(BigDecimal value, BigDecimal total) {
        return value.multiply(ONE_HUNDRED).divide(total, 4, RoundingMode.HALF_UP);
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private record HeldAsset(String ticker, BigDecimal quantity, BigDecimal investedAmount, BigDecimal price,
                             BigDecimal target, AssetCategory category) {

        static HeldAsset of(AssetConsolidation consolidation, BigDecimal price, BigDecimal target,
                            AssetCategory category) {
            return new HeldAsset(consolidation.getTickerSymbol(), consolidation.getTotalQuantity(),
                    consolidation.getInvestedAmount(), price, target, category);
        }

        BigDecimal value() {
            return quantity.multiply(price);
        }

        BigDecimal averagePrice() {
            return investedAmount.divide(quantity, 8, RoundingMode.HALF_UP);
        }

        BigDecimal unrealizedProfit() {
            return value().subtract(investedAmount);
        }

        BigDecimal gainPercentage() {
            return investedAmount.signum() > 0
                    ? unrealizedProfit().multiply(ONE_HUNDRED).divide(investedAmount, 4, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
        }
    }
}
