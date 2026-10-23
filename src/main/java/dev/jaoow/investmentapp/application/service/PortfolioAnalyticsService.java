package dev.jaoow.investmentapp.application.service;

import dev.jaoow.investmentapp.application.dto.response.CategoryAllocationResponse;
import dev.jaoow.investmentapp.application.dto.response.PortfolioClassDetailResponse;
import dev.jaoow.investmentapp.application.dto.response.PortfolioClassResponse;
import dev.jaoow.investmentapp.application.dto.response.PortfolioAssetDetailResponse;
import dev.jaoow.investmentapp.application.dto.response.PortfolioConcentrationResponse;
import dev.jaoow.investmentapp.application.dto.response.PortfolioConcentrationResponse.Alert;
import dev.jaoow.investmentapp.application.dto.response.PortfolioConcentrationResponse.Dimension;
import dev.jaoow.investmentapp.application.dto.response.PortfolioConcentrationResponse.Level;
import dev.jaoow.investmentapp.application.dto.response.PortfolioConcentrationResponse.Limit;
import dev.jaoow.investmentapp.application.dto.response.PortfolioConcentrationResponse.Slice;
import dev.jaoow.investmentapp.application.dto.response.summary.AssetSummaryResponse;
import dev.jaoow.investmentapp.application.dto.response.summary.PortfolioSummaryResponse;
import dev.jaoow.investmentapp.application.exception.ResourceNotFoundException;
import dev.jaoow.investmentapp.application.model.BrapiFields;
import dev.jaoow.investmentapp.application.model.TickerFields;
import dev.jaoow.cotatrack.api.classification.AssetClassifier;
import dev.jaoow.investmentapp.application.service.summary.PortfolioSummaryService;
import dev.jaoow.investmentapp.domain.model.AssetCategory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Service
public class PortfolioAnalyticsService {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal ASSET_WARNING = BigDecimal.valueOf(15);
    private static final BigDecimal ASSET_CRITICAL = BigDecimal.valueOf(25);
    private static final BigDecimal SECTOR_WARNING = BigDecimal.valueOf(30);
    private static final BigDecimal SECTOR_CRITICAL = BigDecimal.valueOf(40);
    private static final BigDecimal CLASS_WARNING = BigDecimal.valueOf(60);
    private static final BigDecimal CLASS_CRITICAL = BigDecimal.valueOf(80);
    private static final String UNCLASSIFIED_SECTOR = "UNCLASSIFIED";

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

    @PreAuthorize("hasRole('USER') and @portfolioSecurity.isOwner(#portfolioId, authentication)")
    public PortfolioConcentrationResponse getConcentration(Long portfolioId) {
        PortfolioSummaryResponse portfolioSummary = portfolioSummaryService.getPortfolioSummary(portfolioId, null);
        List<AssetSummaryResponse> positions = portfolioSummary.getAssetSummaries().stream()
                .filter(asset -> asset.getCurrentValue() != null && asset.getCurrentValue().signum() > 0)
                .sorted(Comparator.comparing(AssetSummaryResponse::getCurrentValue).reversed())
                .toList();
        BigDecimal total = sumValues(positions);

        List<Slice> assets = positions.stream()
                .map(asset -> slice(asset.getTickerSymbol(), assetLabel(asset), List.of(asset), total, null,
                        ASSET_WARNING, ASSET_CRITICAL))
                .toList();

        Map<String, List<AssetSummaryResponse>> bySector = new LinkedHashMap<>();
        positions.forEach(asset -> bySector.computeIfAbsent(sectorOf(asset), ignored -> new ArrayList<>()).add(asset));
        List<Slice> sectors = bySector.entrySet().stream()
                .map(entry -> UNCLASSIFIED_SECTOR.equals(entry.getKey())
                        ? withLevel(slice(entry.getKey(), "Setor não informado", entry.getValue(), total, null,
                        SECTOR_WARNING, SECTOR_CRITICAL), Level.OK)
                        : slice(entry.getKey(), entry.getKey(), entry.getValue(), total, null, SECTOR_WARNING, SECTOR_CRITICAL))
                .sorted(Comparator.comparing(Slice::value).reversed())
                .toList();

        Map<String, PortfolioClassResponse> classSummaries = new LinkedHashMap<>();
        getClasses(portfolioId, portfolioSummary).forEach(item -> classSummaries.put(item.id(), item));
        List<Slice> classes = assetsByCategory(positions).entrySet().stream()
                .map(entry -> {
                    PortfolioClassResponse summary = classSummaries.get(frontendId(entry.getKey()));
                    return slice(frontendId(entry.getKey()), displayName(entry.getKey()), entry.getValue(), total,
                            summary == null ? null : summary.targetPercentage(), CLASS_WARNING, CLASS_CRITICAL);
                })
                .sorted(Comparator.comparing(Slice::value).reversed())
                .toList();

        List<Alert> alerts = Stream.of(
                        alerts(Dimension.ASSET, assets, ASSET_CRITICAL, ASSET_WARNING),
                        alerts(Dimension.SECTOR, sectors, SECTOR_CRITICAL, SECTOR_WARNING),
                        alerts(Dimension.CLASS, classes, CLASS_CRITICAL, CLASS_WARNING))
                .flatMap(List::stream)
                .sorted(Comparator.comparing(Alert::level).reversed()
                        .thenComparing(Comparator.comparing(Alert::percentage).reversed()))
                .toList();
        Level level = alerts.stream().map(Alert::level).max(Comparator.naturalOrder()).orElse(Level.OK);

        return new PortfolioConcentrationResponse(
                total,
                positions.size(),
                effectiveAssets(positions, total),
                assets.isEmpty() ? BigDecimal.ZERO : assets.getFirst().percentage(),
                assets.stream().limit(3).map(Slice::percentage).reduce(BigDecimal.ZERO, BigDecimal::add),
                level,
                assets,
                sectors,
                classes,
                alerts,
                List.of(
                        new Limit(Dimension.ASSET, ASSET_WARNING, ASSET_CRITICAL),
                        new Limit(Dimension.SECTOR, SECTOR_WARNING, SECTOR_CRITICAL),
                        new Limit(Dimension.CLASS, CLASS_WARNING, CLASS_CRITICAL)));
    }

    private Slice slice(String key, String label, List<AssetSummaryResponse> assets, BigDecimal total,
                        BigDecimal targetPercentage, BigDecimal warning, BigDecimal critical) {
        BigDecimal value = sumValues(assets);
        BigDecimal percentage = percentage(value, total);
        Level level = percentage.compareTo(critical) >= 0 ? Level.CRITICAL
                : percentage.compareTo(warning) >= 0 ? Level.WARNING : Level.OK;
        return new Slice(key, label, value, percentage, targetPercentage, assets.size(),
                assets.stream().map(AssetSummaryResponse::getTickerSymbol).toList(), level);
    }

    private Slice withLevel(Slice slice, Level level) {
        return new Slice(slice.key(), slice.label(), slice.value(), slice.percentage(), slice.targetPercentage(),
                slice.assetsCount(), slice.tickers(), level);
    }

    private List<Alert> alerts(Dimension dimension, List<Slice> slices, BigDecimal critical, BigDecimal warning) {
        return slices.stream()
                .filter(slice -> slice.level() != Level.OK)
                .map(slice -> new Alert(dimension, slice.key(), slice.label(), slice.percentage(), slice.level(),
                        alertMessage(dimension, slice, slice.level() == Level.CRITICAL ? critical : warning)))
                .toList();
    }

    private String alertMessage(Dimension dimension, Slice slice, BigDecimal limit) {
        String percentage = formatPercentage(slice.percentage());
        return switch (dimension) {
            case ASSET -> slice.key() + " sozinho representa " + percentage + " do patrimônio (referência: até "
                    + formatPercentage(limit) + " por ativo).";
            case SECTOR -> "O setor " + slice.label() + " soma " + percentage + " do patrimônio em " + slice.assetsCount()
                    + (slice.assetsCount() == 1 ? " ativo" : " ativos") + " (referência: até " + formatPercentage(limit) + " por setor).";
            case CLASS -> slice.label() + " concentra " + percentage + " do patrimônio (referência: até "
                    + formatPercentage(limit) + " por classe).";
        };
    }

    /** Inverse Herfindahl index: how many equally weighted positions would produce the same concentration. */
    private BigDecimal effectiveAssets(List<AssetSummaryResponse> positions, BigDecimal total) {
        if (total.signum() == 0) return BigDecimal.ZERO;
        BigDecimal herfindahl = positions.stream()
                .map(asset -> asset.getCurrentValue().divide(total, 8, RoundingMode.HALF_UP).pow(2))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return BigDecimal.ONE.divide(herfindahl, 1, RoundingMode.HALF_UP);
    }

    private String sectorOf(AssetSummaryResponse asset) {
        TickerFields fields = asset.getTickerFields();
        if (fields != null && fields.getSector() != null && !fields.getSector().isBlank()) return fields.getSector();
        if (fields != null && fields.getSubSector() != null && !fields.getSubSector().isBlank()) return fields.getSubSector();
        return UNCLASSIFIED_SECTOR;
    }

    private String assetLabel(AssetSummaryResponse asset) {
        if (asset.getBrapiFields() != null) return firstAvailableName(asset.getBrapiFields(), asset.getTickerSymbol());
        TickerFields fields = asset.getTickerFields();
        return fields != null && fields.getName() != null ? fields.getName() : asset.getTickerSymbol();
    }

    private static BigDecimal sumValues(List<AssetSummaryResponse> assets) {
        return assets.stream().map(AssetSummaryResponse::getCurrentValue).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static String formatPercentage(BigDecimal value) {
        return value.setScale(1, RoundingMode.HALF_UP).toPlainString().replace('.', ',') + "%";
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