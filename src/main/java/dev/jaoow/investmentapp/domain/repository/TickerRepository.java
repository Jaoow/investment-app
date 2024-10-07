package dev.jaoow.investmentapp.domain.repository;

import dev.jaoow.investmentapp.domain.entity.Ticker;
import dev.jaoow.investmentapp.domain.model.AssetCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;

import java.util.List;

public interface TickerRepository extends JpaRepository<Ticker, String> {

    List<Ticker> findAllByCategory(AssetCategory category);

    List<Ticker> findAllBySector(String sector);

    @Query("SELECT t FROM Ticker t WHERE " +
            "(:sector IS NULL OR t.sector = :sector) AND " +
            "(:subSector IS NULL OR t.subSector = :subSector) AND " +
            "(:category IS NULL OR t.category = :category) AND " +
            "(:symbol IS NULL OR LOWER(t.symbol) LIKE LOWER(CONCAT('%', :symbol, '%')))")
    Page<Ticker> search(
            @Param("symbol") String symbol,
            @Param("category") AssetCategory category,
            @Param("sector") String sector,
            @Param("subSector") String subSector,
            Pageable pageable
    );
    @Query("SELECT DISTINCT t.sector FROM Ticker t")
    List<String> findAllSectors();

    @Query("SELECT DISTINCT t.subSector FROM Ticker t WHERE t.sector = :sector")
    List<String> findAllSubSectorsBySector(@NonNull String sector);

    void deleteBySymbol(String symbol);
}
