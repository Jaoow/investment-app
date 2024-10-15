package dev.jaoow.investmentapp.application.security;

import dev.jaoow.investmentapp.domain.entity.Portfolio;
import dev.jaoow.investmentapp.domain.repository.PortfolioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class PortfolioSecurity {

    private final PortfolioRepository portfolioRepository;

    public PortfolioSecurity(PortfolioRepository portfolioRepository) {
        this.portfolioRepository = portfolioRepository;
    }

    public boolean isOwner(Long portfolioId, Authentication authentication) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new RuntimeException("Portfolio not found"));

        return portfolio.getUser().getEmail().equals(authentication.getName());
    }
}
