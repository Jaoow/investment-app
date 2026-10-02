package dev.jaoow.investmentapp.domain.repository;

import dev.jaoow.investmentapp.domain.entity.PortfolioShare;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PortfolioShareRepository extends JpaRepository<PortfolioShare, Long> {
    Optional<PortfolioShare> findByTokenHash(String tokenHash);

    List<PortfolioShare> findAllByPortfolioId(Long portfolioId);

    Optional<PortfolioShare> findByIdAndPortfolioId(Long id, Long portfolioId);
}
