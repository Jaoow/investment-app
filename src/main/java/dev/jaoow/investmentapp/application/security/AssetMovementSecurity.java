package dev.jaoow.investmentapp.application.security;

import dev.jaoow.investmentapp.domain.repository.AssetMovementRepository;
import dev.jaoow.investmentapp.domain.repository.PortfolioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class AssetMovementSecurity {

    private final AssetMovementRepository assetMovementRepository;
    private final PortfolioRepository portfolioRepository;

    public AssetMovementSecurity(AssetMovementRepository assetMovementRepository, PortfolioRepository portfolioRepository) {
        this.assetMovementRepository = assetMovementRepository;
        this.portfolioRepository = portfolioRepository;
    }

    public boolean isOwner(Long movementId, Authentication authentication) {
        return assetMovementRepository.findById(movementId)
                .map(assetMovement -> {
                    String userEmail = authentication.getName();
                    Long portfolioId = assetMovement.getPortfolio().getId();
                    return portfolioRepository.findById(portfolioId)
                            .map(portfolio -> portfolio.getUser().getEmail().equals(userEmail))
                            .orElse(false);
                })
                .orElse(false);
    }
}
