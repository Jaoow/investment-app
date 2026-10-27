package dev.jaoow.investmentapp.application.service.consolidation;

import dev.jaoow.investmentapp.application.exception.InvalidPortfolioPositionException;
import dev.jaoow.investmentapp.application.model.AssetConsolidation;
import dev.jaoow.investmentapp.domain.entity.AssetMovement;
import dev.jaoow.investmentapp.domain.entity.Portfolio;
import dev.jaoow.investmentapp.domain.model.MovementType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AssetConsolidationServiceTests {

    private final AssetConsolidationService service = new AssetConsolidationService();

    @Test
    void netsPurchasesAndSalesIntoCurrentQuantity() {
        Portfolio portfolio = new Portfolio();
        portfolio.setAssetMovements(List.of(
                movement("ABC", "5", "10", MovementType.BUY),
                movement("ABC", "2", "12", MovementType.SELL)
        ));

        List<AssetConsolidation> result = service.processAssetMovements(portfolio);

        assertEquals(1, result.size());
        assertEquals(new BigDecimal("3"), result.getFirst().getTotalQuantity());
    }

    @Test
    void rejectsOversoldPositionsInsteadOfSilentlyDroppingThem() {
        Portfolio portfolio = new Portfolio();
        portfolio.setAssetMovements(List.of(
                movement("ABC", "2", "10", MovementType.BUY),
                movement("ABC", "3", "12", MovementType.SELL)
        ));

        assertThrows(InvalidPortfolioPositionException.class, () -> service.processAssetMovements(portfolio));
    }

    @Test
    void includesOperationCostInInvestedAmount() {
        AssetMovement purchase = movement("ABC", "2", "10", MovementType.BUY);
        purchase.setOperationCost(new BigDecimal("3.50"));
        Portfolio portfolio = new Portfolio();
        portfolio.setAssetMovements(List.of(purchase));

        AssetConsolidation result = service.processAssetMovements(portfolio).getFirst();

        assertEquals(new BigDecimal("23.50"), result.getInvestedAmount());
    }

    @Test
    void partialSaleDoesNotTurnSmallAdjustedCostNegative() {
        AssetMovement adjustment = new AssetMovement();
        adjustment.setTickerSymbol("ABC");
        adjustment.setType(MovementType.POSITION_ADJUSTMENT);
        adjustment.setAdjustmentQuantity(new BigDecimal("3"));
        adjustment.setAdjustmentTotalCost(new BigDecimal("0.00000002"));
        Portfolio portfolio = new Portfolio();
        portfolio.setAssetMovements(List.of(adjustment, movement("ABC", "2.9", "1", MovementType.SELL)));

        AssetConsolidation result = service.processAssetMovements(portfolio).getFirst();

        assertEquals(0, new BigDecimal("0.1").compareTo(result.getTotalQuantity()));
        assertEquals(0, new BigDecimal("0.0000000006666667").compareTo(result.getInvestedAmount()));
    }

    private AssetMovement movement(String ticker, String quantity, String price, MovementType type) {
        AssetMovement movement = new AssetMovement();
        movement.setTickerSymbol(ticker);
        movement.setQuantity(new BigDecimal(quantity));
        movement.setPrice(new BigDecimal(price));
        movement.setType(type);
        return movement;
    }
}
