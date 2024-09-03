package dev.jaoow.investmentapp.domain.repository;

import dev.jaoow.investmentapp.domain.entity.CategoryAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryAllocationRepository extends JpaRepository<CategoryAllocation, Long> {

    List<CategoryAllocation> findByPortfolioId(Long portfolioId);

    void deleteAllByPortfolioId(Long portfolioId);
}
