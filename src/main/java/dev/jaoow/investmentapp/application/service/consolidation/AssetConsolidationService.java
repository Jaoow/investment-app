package dev.jaoow.investmentapp.application.service.consolidation;

import dev.jaoow.investmentapp.application.model.AssetConsolidation;
import dev.jaoow.investmentapp.application.exception.InvalidPortfolioPositionException;
import dev.jaoow.investmentapp.domain.entity.AssetMovement;
import dev.jaoow.investmentapp.domain.entity.Portfolio;
import dev.jaoow.investmentapp.domain.model.MovementType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
                    if (a.getId() == null && b.getId() == null) return 0;
                    if (a.getId() == null) return 1;
                    if (b.getId() == null) return -1;
                    return a.getId().compareTo(b.getId());
                })
                .toList();

        for (AssetMovement movement : sortedMovements) {
            String tickerSymbol = movement.getTickerSymbol();

            AssetConsolidation consolidation = assetConsolidationMap
                    .computeIfAbsent(tickerSymbol, AssetConsolidation::new);

            if (movement.getType() == MovementType.POSITION_ADJUSTMENT) {
                consolidation.setTotalQuantity(movement.getAdjustmentQuantity());
                consolidation.setInvestedAmount(movement.getAdjustmentTotalCost());
            } else if (movement.getType() == MovementType.BUY) {
                BigDecimal movementTotal = movement.getQuantity().multiply(movement.getPrice());
                if (movement.getOperationCost() != null) {
                    movementTotal = movementTotal.add(movement.getOperationCost());
                }
                consolidation.addInvestment(movementTotal);
                consolidation.addQuantity(movement.getQuantity());
            } else if (movement.getType() == MovementType.SELL) {
                if (movement.getQuantity().compareTo(consolidation.getTotalQuantity()) > 0) {
                    throw new InvalidPortfolioPositionException(
                            "Movements result in a negative position for " + tickerSymbol + ".");
                }
                BigDecimal investedAmountToSubtract =
                        movement.getQuantity().compareTo(consolidation.getTotalQuantity()) == 0
                                ? consolidation.getInvestedAmount()
                                : consolidation.getInvestedAmount().multiply(movement.getQuantity())
                                        .divide(consolidation.getTotalQuantity(), 16, RoundingMode.HALF_UP);
                consolidation.subtractInvestment(investedAmountToSubtract);
                consolidation.subtractQuantity(movement.getQuantity());
                if (consolidation.getTotalQuantity().signum() == 0) {
                    consolidation.setInvestedAmount(BigDecimal.ZERO);
                }
            } else {
                throw new InvalidPortfolioPositionException("Unknown movement type for " + tickerSymbol + ".");
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
