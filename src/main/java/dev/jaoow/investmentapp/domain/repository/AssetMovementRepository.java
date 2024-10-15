package dev.jaoow.investmentapp.domain.repository;

import dev.jaoow.investmentapp.domain.entity.AssetMovement;
import dev.jaoow.investmentapp.domain.entity.Portfolio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AssetMovementRepository extends JpaRepository<AssetMovement, Long> {

    Page<AssetMovement> findAllByPortfolio(Portfolio portfolio, Pageable pageable);

}
