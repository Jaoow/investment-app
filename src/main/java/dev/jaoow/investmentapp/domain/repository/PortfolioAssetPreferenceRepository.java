package dev.jaoow.investmentapp.domain.repository;

import dev.jaoow.investmentapp.domain.entity.PortfolioAssetPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PortfolioAssetPreferenceRepository extends JpaRepository<PortfolioAssetPreference, Long> {
    Optional<PortfolioAssetPreference> findByPortfolioIdAndTickerSymbol(Long portfolioId, String tickerSymbol);

    List<PortfolioAssetPreference> findAllByPortfolioId(Long portfolioId);

    void deleteByPortfolioIdAndTickerSymbol(Long portfolioId, String tickerSymbol);
}
