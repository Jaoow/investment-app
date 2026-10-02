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

    private AssetMovement movement(String ticker, String quantity, String price, MovementType type) {
        AssetMovement movement = new AssetMovement();
        movement.setTickerSymbol(ticker);
        movement.setQuantity(new BigDecimal(quantity));
        movement.setPrice(new BigDecimal(price));
        movement.setType(type);
        return movement;
    }
}
