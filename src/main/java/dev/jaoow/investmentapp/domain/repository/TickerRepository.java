package dev.jaoow.investmentapp.domain.repository;

import dev.jaoow.investmentapp.domain.model.AssetCategory;
import dev.jaoow.investmentapp.domain.entity.Ticker;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TickerRepository extends JpaRepository<Ticker, String> {
    List<Ticker> findAllByCategory(AssetCategory category);
}
