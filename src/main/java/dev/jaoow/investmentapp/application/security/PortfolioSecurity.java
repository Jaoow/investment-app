package dev.jaoow.investmentapp.application.security;

import dev.jaoow.investmentapp.application.exception.PortfolioNotFoundException;
import dev.jaoow.investmentapp.domain.entity.Portfolio;
import dev.jaoow.investmentapp.domain.repository.PortfolioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@SuppressWarnings("unused")
public class PortfolioSecurity {

    private final PortfolioRepository portfolioRepository;

    public PortfolioSecurity(PortfolioRepository portfolioRepository) {
        this.portfolioRepository = portfolioRepository;
    }

    public boolean isOwner(Long portfolioId, Authentication authentication) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new PortfolioNotFoundException(portfolioId));

        return portfolio.getUser().getEmail().equals(authentication.getName());
    }
}
