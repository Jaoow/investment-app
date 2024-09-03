package dev.jaoow.investmentapp.domain.repository;

import dev.jaoow.investmentapp.domain.entity.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {
}
