package dev.jaoow.investmentapp.domain.repository;

import dev.jaoow.investmentapp.domain.entity.AssetMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AssetMovementRepository extends JpaRepository<AssetMovement, Long> {
}
