package dev.jaoow.investmentapp.application.service;

import dev.jaoow.investmentapp.application.dto.response.CategoryAllocationResponse;
import dev.jaoow.investmentapp.application.dto.response.PortfolioClassDetailResponse;
import dev.jaoow.investmentapp.application.dto.response.PortfolioClassResponse;
import dev.jaoow.investmentapp.application.dto.response.PortfolioAssetDetailResponse;
import dev.jaoow.investmentapp.application.dto.response.summary.AssetSummaryResponse;
import dev.jaoow.investmentapp.application.dto.response.summary.PortfolioSummaryResponse;
import dev.jaoow.investmentapp.application.exception.ResourceNotFoundException;
import dev.jaoow.investmentapp.application.model.BrapiFields;
import dev.jaoow.investmentapp.application.model.TickerFields;
import dev.jaoow.cotatrack.api.classification.AssetClassifier;
import dev.jaoow.investmentapp.application.service.summary.PortfolioSummaryService;
import dev.jaoow.investmentapp.domain.model.AssetCategory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class PortfolioAnalyticsService {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private final PortfolioSummaryService portfolioSummaryService;
    private final AllocationService allocationService;

    public PortfolioAnalyticsService(PortfolioSummaryService portfolioSummaryService, AllocationService allocationService) {
        this.portfolioSummaryService = portfolioSummaryService;
        this.allocationService = allocationService;
    }

    public List<PortfolioClassResponse> getClasses(Long portfolioId) {
        PortfolioSummaryResponse portfolioSummary = portfolioSummaryService.getPortfolioSummary(portfolioId, null);
        return getClasses(portfolioId, portfolioSummary);
    }

    public List<PortfolioClassResponse> getClasses(Long portfolioId, PortfolioSummaryResponse portfolioSummary) {
        List<CategoryAllocationResponse> allocations = allocationService.getAllocationsWithZeroValues(portfolioId);
        Map<AssetCategory, List<AssetSummaryResponse>> assetsByCategory = assetsByCategory(portfolioSummary.getAssetSummaries());
        BigDecimal portfolioValue = portfolioSummary.getCurrentValue();

        return java.util.Arrays.stream(AssetCategory.values())
                .map(category -> summarizeClass(category, assetsByCategory.getOrDefault(category, List.of()),
                        allocations, portfolioValue))
                .toList();
    }

    public PortfolioClassDetailResponse getClassDetail(Long portfolioId, String classId) {
        AssetCategory category = resolveCategory(classId);
        PortfolioSummaryResponse portfolioSummary = portfolioSummaryService.getPortfolioSummary(portfolioId, null);
        List<CategoryAllocationResponse> allocations = allocationService.getAllocationsWithZeroValues(portfolioId);
        List<AssetSummaryResponse> classAssets = assetsByCategory(portfolioSummary.getAssetSummaries())
            .getOrDefault(category, List.of());
        PortfolioClassResponse summary = summarizeClass(category, classAssets, allocations, portfolioSummary.getCurrentValue());
        return new PortfolioClassDetailResponse(summary, classAssets);
    }

        public PortfolioAssetDetailResponse getAssetDetail(Long portfolioId, String ticker) {
        PortfolioSummaryResponse portfolio = portfolioSummaryService.getPortfolioSummary(portfolioId, null);
        AssetSummaryResponse asset = portfolio.getAssetSummaries().stream()
            .filter(item -> item.getTickerSymbol().equalsIgnoreCase(ticker))
            .findFirst()
            .orElseThrow(() -> new ResourceNotFoundException("Asset position not found: " + ticker));
        TickerFields tickerFields = asset.getTickerFields();
        if (tickerFields == null || tickerFields.getCategory() == null) {
            String catName = AssetClassifier.classify(ticker, null, null, null, null, null).getCategoryName();
            if (tickerFields == null) {
                tickerFields = new TickerFields();
                tickerFields.setName(ticker);
                asset.setTickerFields(tickerFields);
            }
            tickerFields.setCategory(catName);
        }

        AssetCategory category = resolveCategory(tickerFields.getCategory());
        List<CategoryAllocationResponse> allocations = allocationService.getAllocationsWithZeroValues(portfolioId);
        CategoryAllocationResponse categoryAllocation = allocations.stream()
            .filter(item -> item.getCategory() == category)
            .findFirst()
            .orElse(null);
        BigDecimal targetInClass = categoryAllocation == null ? BigDecimal.ZERO : categoryAllocation.getAssetAllocations().stream()
            .filter(item -> item.getTickerSymbol().equalsIgnoreCase(ticker))
            .map(CategoryAllocationResponse.AssetAllocationResponse::getTargetPercentage)
            .findFirst()
            .orElse(BigDecimal.ZERO);
        BigDecimal categoryTarget = categoryAllocation == null ? BigDecimal.ZERO : categoryAllocation.getCategoryTargetPercentage();
        BigDecimal currentValue = asset.getCurrentValue();
        BigDecimal classValue = portfolio.getAssetSummaries().stream()
            .filter(item -> item.getTickerFields() != null && category.name().equals(item.getTickerFields().getCategory()))
            .map(AssetSummaryResponse::getCurrentValue)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal portfolioTarget = categoryTarget.multiply(targetInClass).divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);
        BrapiFields quote = asset.getBrapiFields();
        String shortName = quote != null && quote.getShortName() != null ? quote.getShortName() : ticker;
        String longName = quote != null && quote.getLongName() != null ? quote.getLongName() : 
                          (tickerFields.getName() != null && !tickerFields.getName().equalsIgnoreCase(ticker) ? tickerFields.getName() : null);
        String name = (longName != null && !longName.isBlank() && !longName.equalsIgnoreCase(ticker))
                ? longName
                : (tickerFields.getName() != null ? tickerFields.getName() : (quote == null ? ticker : firstAvailableName(quote, ticker)));
        String logoUrl = tickerFields.getLogoUrl() != null ? tickerFields.getLogoUrl() : 
                         (quote != null ? quote.getLogourl() : null);
        String sector = tickerFields.getSector() == null ? tickerFields.getSubSector() : tickerFields.getSector();

        return new PortfolioAssetDetailResponse(
            asset.getTickerSymbol(),
            name,
            shortName,
            longName,
            logoUrl,
            sector == null ? "Não informado" : sector,
            asset.getQuantity(),
            asset.getAveragePrice(),
                quote == null ? currentValue.divide(asset.getQuantity(), 8, RoundingMode.HALF_UP) : quote.getRegularMarketPrice(),
            asset.getTotalInvested(),
            currentValue,
            asset.getProfitOrLoss(),
            asset.getPercentageChange(),
                percentage(currentValue, classValue),
            targetInClass,
            percentage(currentValue, portfolio.getCurrentValue()),
            portfolioTarget,
            quote == null ? null : quote.getRegularMarketChangePercent());
        }

    private PortfolioClassResponse summarizeClass(
            AssetCategory category,
            List<AssetSummaryResponse> assets,
            List<CategoryAllocationResponse> allocations,
            BigDecimal portfolioValue) {
        BigDecimal invested = assets.stream().map(AssetSummaryResponse::getTotalInvested).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal currentValue = assets.stream().map(AssetSummaryResponse::getCurrentValue).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal profit = currentValue.subtract(invested);
        BigDecimal target = allocations.stream()
                .filter(allocation -> allocation.getCategory() == category)
                .map(CategoryAllocationResponse::getCategoryTargetPercentage)
                .findFirst()
                .orElse(BigDecimal.ZERO);

        return new PortfolioClassResponse(
                frontendId(category),
                displayName(category),
                percentage(currentValue, portfolioValue),
                target,
                currentValue,
                percentage(profit, invested),
                assets.size(),
                invested,
                profit);
    }

    private Map<AssetCategory, List<AssetSummaryResponse>> assetsByCategory(List<AssetSummaryResponse> assets) {
        Map<AssetCategory, List<AssetSummaryResponse>> grouped = new EnumMap<>(AssetCategory.class);
        for (AssetSummaryResponse asset : assets) {
            AssetCategory category = null;
            if (asset.getTickerFields() != null && asset.getTickerFields().getCategory() != null) {
                try {
                    category = AssetCategory.valueOf(asset.getTickerFields().getCategory());
                } catch (IllegalArgumentException ignored) {
                }
            }
            if (category == null) {
                String catName = AssetClassifier.classify(asset.getTickerSymbol(), null, null, null, null, null).getCategoryName();
                try {
                    category = AssetCategory.valueOf(catName);
                } catch (IllegalArgumentException | NullPointerException e) {
                    category = AssetCategory.EQUITIES;
                }
            }
            grouped.computeIfAbsent(category, ignored -> new ArrayList<>()).add(asset);
        }
        return grouped;
    }

    private BigDecimal percentage(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return numerator.multiply(ONE_HUNDRED).divide(denominator, 2, RoundingMode.HALF_UP);
    }

    private AssetCategory resolveCategory(String classId) {
        for (AssetCategory category : AssetCategory.values()) {
            if (category.name().equalsIgnoreCase(classId) || frontendId(category).equalsIgnoreCase(classId)) return category;
        }
        throw new ResourceNotFoundException("Unknown asset class: " + classId);
    }

    private String firstAvailableName(BrapiFields quote, String ticker) {
        if (quote.getLongName() != null && !quote.getLongName().isBlank()) return quote.getLongName();
        if (quote.getShortName() != null && !quote.getShortName().isBlank()) return quote.getShortName();
        return ticker;
    }

    private String frontendId(AssetCategory category) {
        return switch (category) {
            case EQUITIES -> "equities";
            case REAL_ESTATE_FUNDS -> "realEstate";
            case ETFS -> "etfs";
            case BDRS -> "bdrs";
            case FIXED_INCOME -> "fixedIncome";
            case TREASURY -> "treasury";
            case OTHERS -> "others";
        };
    }

    private String displayName(AssetCategory category) {
        return switch (category) {
            case EQUITIES -> "Ações";
            case REAL_ESTATE_FUNDS -> "FIIs";
            case ETFS -> "ETFs";
            case BDRS -> "BDRs";
            case FIXED_INCOME -> "Renda fixa";
            case TREASURY -> "Tesouro Direto";
            case OTHERS -> "Outros";
        };
    }
}