package dev.jaoow.investmentapp.application.service.consolidation;

import dev.jaoow.investmentapp.application.model.AssetConsolidation;
import dev.jaoow.investmentapp.application.exception.InvalidPortfolioPositionException;
import dev.jaoow.investmentapp.domain.entity.AssetMovement;
import dev.jaoow.investmentapp.domain.entity.Portfolio;
import dev.jaoow.investmentapp.domain.model.MovementType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AssetConsolidationService {

    public List<AssetConsolidation> processAssetMovements(Portfolio portfolio) {
        Map<String, AssetConsolidation> assetConsolidationMap = new HashMap<>();

        // Sort movements by date and ID to ensure chronological processing
        List<AssetMovement> sortedMovements = portfolio.getAssetMovements().stream()
                .sorted((a, b) -> {
                    if (a.getDate() == null || b.getDate() == null) return 0;
                    int dateCompare = a.getDate().compareTo(b.getDate());
                    if (dateCompare != 0) return dateCompare;
                    if (a.getId() == null || b.getId() == null) return 0;
                    return a.getId().compareTo(b.getId());
                })
                .toList();

        for (AssetMovement movement : sortedMovements) {
            String tickerSymbol = movement.getTickerSymbol();

            AssetConsolidation consolidation = assetConsolidationMap
                    .computeIfAbsent(tickerSymbol, AssetConsolidation::new);

            if (movement.getType() == MovementType.BUY) {
                BigDecimal movementTotal = movement.getQuantity().multiply(movement.getPrice());
                consolidation.addInvestment(movementTotal);
                consolidation.addQuantity(movement.getQuantity());
            } else if (movement.getType() == MovementType.SELL) {
                BigDecimal averagePrice = BigDecimal.ZERO;
                if (consolidation.getTotalQuantity().compareTo(BigDecimal.ZERO) > 0) {
                    averagePrice = consolidation.getInvestedAmount().divide(consolidation.getTotalQuantity(), 8, java.math.RoundingMode.HALF_UP);
                }
                
                BigDecimal investedAmountToSubtract = movement.getQuantity().multiply(averagePrice);
                consolidation.subtractInvestment(investedAmountToSubtract);
                consolidation.subtractQuantity(movement.getQuantity());
            }
        }

        for (AssetConsolidation consolidation : assetConsolidationMap.values()) {
            if (consolidation.getTotalQuantity().compareTo(BigDecimal.ZERO) < 0) {
                throw new InvalidPortfolioPositionException(
                        "Movements result in a negative position for " + consolidation.getTickerSymbol() + ".");
            }
        }

        return assetConsolidationMap.values().stream()
                .filter(AssetConsolidation::isValid)
                .toList();
    }
}
