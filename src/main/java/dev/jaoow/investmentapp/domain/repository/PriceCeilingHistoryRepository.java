package dev.jaoow.investmentapp.domain.repository;

import dev.jaoow.investmentapp.domain.entity.PriceCeilingHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PriceCeilingHistoryRepository extends JpaRepository<PriceCeilingHistory, Long> {
    Page<PriceCeilingHistory> findByPortfolioIdAndTickerSymbolOrderByChangedAtDescIdDesc(
            Long portfolioId, String tickerSymbol, Pageable pageable);
}
