package dev.jaoow.investmentapp.domain.repository;

import dev.jaoow.investmentapp.domain.entity.AssetMovement;
import dev.jaoow.investmentapp.domain.entity.Portfolio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface AssetMovementRepository extends JpaRepository<AssetMovement, Long> {

    Page<AssetMovement> findAllByPortfolio(Portfolio portfolio, Pageable pageable);

    void deleteAllByPortfolio(Portfolio portfolio);

    java.util.List<AssetMovement> findAllByPortfolioAndExternalReferenceIn(Portfolio portfolio, java.util.Collection<String> externalReferences);

    @Query("SELECT COALESCE(SUM(CASE WHEN m.type = dev.jaoow.investmentapp.domain.model.MovementType.BUY "
            + "THEN m.quantity ELSE -m.quantity END), 0) FROM AssetMovement m "
            + "WHERE m.portfolio.id = :portfolioId AND m.tickerSymbol = :tickerSymbol")
    BigDecimal sumNetQuantity(@Param("portfolioId") Long portfolioId, @Param("tickerSymbol") String tickerSymbol);

}
