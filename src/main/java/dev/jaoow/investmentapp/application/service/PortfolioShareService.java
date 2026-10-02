package dev.jaoow.investmentapp.application.service;

import dev.jaoow.investmentapp.application.dto.request.CreatePortfolioShareRequest;
import dev.jaoow.investmentapp.application.dto.response.PortfolioShareCreatedResponse;
import dev.jaoow.investmentapp.application.dto.response.PortfolioShareResponse;
import dev.jaoow.investmentapp.application.dto.response.SharedPortfolioSnapshotResponse;
import dev.jaoow.investmentapp.application.exception.PortfolioNotFoundException;
import dev.jaoow.investmentapp.application.exception.InvalidPortfolioShareException;
import dev.jaoow.investmentapp.application.exception.ResourceNotFoundException;
import dev.jaoow.investmentapp.application.model.AssetConsolidation;
import dev.jaoow.investmentapp.application.model.MarketQuote;
import dev.jaoow.investmentapp.application.service.consolidation.AssetConsolidationService;
import dev.jaoow.investmentapp.domain.entity.Portfolio;
import dev.jaoow.investmentapp.domain.entity.PortfolioShare;
import dev.jaoow.investmentapp.domain.repository.PortfolioRepository;
import dev.jaoow.investmentapp.domain.repository.PortfolioShareRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Base64;
import java.util.List;

@Service
public class PortfolioShareService {

    private static final String PUBLIC_PATH = "/public/portfolio-shares/";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final PortfolioShareRepository shareRepository;
    private final PortfolioRepository portfolioRepository;
    private final AssetConsolidationService consolidationService;
    private final MarketQuoteProvider marketQuoteProvider;
    private final MarketQuoteFreshness marketQuoteFreshness;
    private final Clock clock;

    public PortfolioShareService(
            PortfolioShareRepository shareRepository,
            PortfolioRepository portfolioRepository,
            AssetConsolidationService consolidationService,
            MarketQuoteProvider marketQuoteProvider,
            MarketQuoteFreshness marketQuoteFreshness,
            Clock clock) {
        this.shareRepository = shareRepository;
        this.portfolioRepository = portfolioRepository;
        this.consolidationService = consolidationService;
        this.marketQuoteProvider = marketQuoteProvider;
        this.marketQuoteFreshness = marketQuoteFreshness;
        this.clock = clock;
    }

    @Transactional
    @PreAuthorize("hasRole('USER') and @portfolioSecurity.isOwner(#portfolioId, authentication)")
    public PortfolioShareCreatedResponse create(Long portfolioId, CreatePortfolioShareRequest request) {
        if (request.getExpiresAt() == null || !request.getExpiresAt().isAfter(clock.instant())) {
            throw new InvalidPortfolioShareException("Share expiration must be in the future.");
        }
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new PortfolioNotFoundException(portfolioId));
        String token = generateToken();

        PortfolioShare share = new PortfolioShare();
        share.setPortfolio(portfolio);
        share.setTokenHash(hashToken(token));
        share.setIncludeValues(request.isIncludeValues());
        share.setExpiresAt(request.getExpiresAt());
        share.setCreatedAt(clock.instant());
        share = shareRepository.save(share);

        return new PortfolioShareCreatedResponse(
                share.getId(),
                PUBLIC_PATH + token,
                share.getExpiresAt(),
                share.isIncludeValues()
        );
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('USER') and @portfolioSecurity.isOwner(#portfolioId, authentication)")
    public List<PortfolioShareResponse> list(Long portfolioId) {
        return shareRepository.findAllByPortfolioId(portfolioId).stream()
                .map(share -> new PortfolioShareResponse(
                        share.getId(),
                        share.getExpiresAt(),
                        share.getCreatedAt(),
                        share.getRevokedAt(),
                        share.isIncludeValues()))
                .toList();
    }

    @Transactional
    @PreAuthorize("hasRole('USER') and @portfolioSecurity.isOwner(#portfolioId, authentication)")
    public void revoke(Long portfolioId, Long shareId) {
        PortfolioShare share = shareRepository.findByIdAndPortfolioId(shareId, portfolioId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio share not found."));
        if (share.getRevokedAt() == null) {
            share.setRevokedAt(clock.instant());
            shareRepository.save(share);
        }
    }

    @Transactional(readOnly = true)
    public SharedPortfolioSnapshotResponse readPublicSnapshot(String token) {
        PortfolioShare share = shareRepository.findByTokenHash(hashToken(token))
                .filter(candidate -> candidate.getRevokedAt() == null)
                .filter(candidate -> candidate.getExpiresAt().isAfter(clock.instant()))
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio share not found."));
        Portfolio portfolio = share.getPortfolio();
        List<AssetConsolidation> positions = consolidationService.processAssetMovements(portfolio);

        SharedPortfolioSnapshotResponse response = new SharedPortfolioSnapshotResponse();
        response.setGeneratedAt(clock.instant());
        response.setExpiresAt(share.getExpiresAt());
        response.setIncludesValues(share.isIncludeValues());
        response.setAssets(positions.stream()
                .map(position -> toSharedAsset(position, share.isIncludeValues()))
                .toList());
        return response;
    }

    private SharedPortfolioSnapshotResponse.SharedAssetResponse toSharedAsset(
            AssetConsolidation position,
            boolean includeValues) {
        SharedPortfolioSnapshotResponse.SharedAssetResponse response =
                new SharedPortfolioSnapshotResponse.SharedAssetResponse();
        response.setTickerSymbol(position.getTickerSymbol());
        response.setQuantity(position.getTotalQuantity());
        if (includeValues) {
            MarketQuote quote = marketQuoteFreshness.requireFresh(marketQuoteProvider.getQuote(position.getTickerSymbol()));
            response.setUnitPrice(quote.price());
            response.setMarketValue(position.getTotalQuantity().multiply(quote.price()));
            response.setCurrency(quote.currency());
            response.setQuoteProvider(quote.provider());
            response.setQuoteObservedAt(quote.observedAt());
        }
        return response;
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable.", ex);
        }
    }
}
