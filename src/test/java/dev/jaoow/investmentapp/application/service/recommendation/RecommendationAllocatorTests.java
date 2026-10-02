package dev.jaoow.investmentapp.application.service.recommendation;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RecommendationAllocatorTests {

    private final RecommendationAllocator allocator = new RecommendationAllocator();

    @Test
    void allocatesWholeUnitsAndUsesResidualForNextPriority() {
        PurchaseCandidate abc = candidate("ABC", "100", "500", "0.8");
        PurchaseCandidate xyz = candidate("XYZ", "50", "500", "0.6");

        List<RecommendationAllocation> result = allocator.allocate(new BigDecimal("150"), List.of(abc, xyz));

        assertEquals(2, result.size());
        assertEquals(new BigDecimal("1"), quantityFor(result, "ABC"));
        assertEquals(new BigDecimal("1"), quantityFor(result, "XYZ"));
        assertEquals(new BigDecimal("150"), totalValue(result));
    }

    @Test
    void leavesBudgetAsResidualWhenNoWholeUnitFits() {
        List<RecommendationAllocation> result = allocator.allocate(
                new BigDecimal("90"),
                List.of(candidate("ABC", "100", "500", "1.0")));

        assertEquals(0, result.size());
    }

    @Test
    void neverAllocatesBeyondTargetHeadroomOrBudget() {
        List<RecommendationAllocation> result = allocator.allocate(
                new BigDecimal("200"),
                List.of(candidate("ABC", "50", "60", "1.0")));

        assertEquals(new BigDecimal("1"), quantityFor(result, "ABC"));
        assertEquals(new BigDecimal("50"), totalValue(result));
    }

    private PurchaseCandidate candidate(String ticker, String price, String gap, String score) {
        return new PurchaseCandidate(
                ticker,
                new BigDecimal(price),
                new BigDecimal("1000"),
                new BigDecimal(score),
                BigDecimal.ZERO,
                BigDecimal.valueOf(100),
                BigDecimal.ZERO,
                null,
                new BigDecimal(gap),
                List.of()
        );
    }

    private BigDecimal quantityFor(List<RecommendationAllocation> result, String ticker) {
        return result.stream()
                .filter(allocation -> allocation.candidate().tickerSymbol().equals(ticker))
                .map(RecommendationAllocation::quantity)
                .findFirst()
                .orElse(BigDecimal.ZERO);
    }

    private BigDecimal totalValue(List<RecommendationAllocation> result) {
        return result.stream()
                .map(RecommendationAllocation::estimatedValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
