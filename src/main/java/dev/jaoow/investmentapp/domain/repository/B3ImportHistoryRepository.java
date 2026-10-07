package dev.jaoow.investmentapp.domain.repository;

import dev.jaoow.investmentapp.domain.entity.B3ImportHistory;
import dev.jaoow.investmentapp.domain.entity.Portfolio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface B3ImportHistoryRepository extends JpaRepository<B3ImportHistory, Long> {
    Page<B3ImportHistory> findAllByPortfolio(Portfolio portfolio, Pageable pageable);
    Optional<B3ImportHistory> findByPortfolioIdAndFileHash(Long portfolioId, String fileHash);
}
