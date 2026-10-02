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

        for (AssetMovement movement : portfolio.getAssetMovements()) {
            String tickerSymbol = movement.getTickerSymbol();

            AssetConsolidation consolidation = assetConsolidationMap
                    .computeIfAbsent(tickerSymbol, AssetConsolidation::new);

            BigDecimal movementTotal = movement.getQuantity().multiply(movement.getPrice());

            if (movement.getType() == MovementType.BUY) {
                consolidation.addInvestment(movementTotal);
                consolidation.addQuantity(movement.getQuantity());
            } else if (movement.getType() == MovementType.SELL) {
                consolidation.subtractInvestment(movementTotal);
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
