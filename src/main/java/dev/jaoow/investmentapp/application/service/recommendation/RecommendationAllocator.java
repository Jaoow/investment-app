package dev.jaoow.investmentapp.application.service.recommendation;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class RecommendationAllocator {

    public List<RecommendationAllocation> allocate(BigDecimal amount, List<PurchaseCandidate> candidates) {
        List<MutableAllocation> allocations = candidates.stream()
                .filter(candidate -> candidate.score().compareTo(BigDecimal.ZERO) > 0)
                .filter(candidate -> candidate.targetGapAmount().compareTo(candidate.price()) >= 0)
                .sorted(Comparator.comparing(PurchaseCandidate::score).reversed()
                        .thenComparing(PurchaseCandidate::targetPercentage, Comparator.reverseOrder())
                        .thenComparing(PurchaseCandidate::tickerSymbol))
                .map(MutableAllocation::new)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

        List<MutableAllocation> active = new ArrayList<>(allocations);
        BigDecimal remaining = amount;
        while (!active.isEmpty() && remaining.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal currentBudget = remaining;
            BigDecimal totalScore = active.stream()
                    .map(entry -> entry.candidate.score())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (totalScore.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            List<MutableAllocation> capped = active.stream()
                    .filter(entry -> entry.remainingCapacity.compareTo(
                            currentBudget.multiply(entry.candidate.score()).divide(totalScore, 12, RoundingMode.DOWN)) <= 0)
                    .toList();
            if (!capped.isEmpty()) {
                for (MutableAllocation entry : capped) {
                    BigDecimal share = remaining.multiply(entry.candidate.score())
                            .divide(totalScore, 12, RoundingMode.DOWN);
                    BigDecimal spendLimit = share.min(entry.remainingCapacity).min(remaining);
                    BigDecimal quantity = integerUnits(spendLimit, entry.candidate.price());
                    entry.add(quantity);
                    remaining = remaining.subtract(quantity.multiply(entry.candidate.price()));
                    active.remove(entry);
                }
                continue;
            }

            for (MutableAllocation entry : List.copyOf(active)) {
                BigDecimal share = currentBudget.multiply(entry.candidate.score())
                        .divide(totalScore, 12, RoundingMode.DOWN);
                BigDecimal spendLimit = share.min(entry.remainingCapacity).min(remaining);
                BigDecimal quantity = integerUnits(spendLimit, entry.candidate.price());
                entry.add(quantity);
                remaining = remaining.subtract(quantity.multiply(entry.candidate.price()));
            }

            allocateResidualByPriority(active, remaining);
            remaining = amount.subtract(allocations.stream()
                    .map(entry -> entry.allocatedValue)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            break;
        }

        return allocations.stream()
                .filter(entry -> entry.quantity.compareTo(BigDecimal.ZERO) > 0)
                .map(entry -> new RecommendationAllocation(entry.candidate, entry.quantity, entry.allocatedValue))
                .toList();
    }

    private void allocateResidualByPriority(List<MutableAllocation> active, BigDecimal remaining) {
        BigDecimal residual = remaining;
        for (MutableAllocation entry : active) {
            BigDecimal spendLimit = residual.min(entry.remainingCapacity);
            BigDecimal quantity = integerUnits(spendLimit, entry.candidate.price());
            entry.add(quantity);
            residual = residual.subtract(quantity.multiply(entry.candidate.price()));
        }
    }

    private BigDecimal integerUnits(BigDecimal amount, BigDecimal unitPrice) {
        return amount.divideToIntegralValue(unitPrice).setScale(0, RoundingMode.DOWN);
    }

    private static final class MutableAllocation {
        private final PurchaseCandidate candidate;
        private BigDecimal remainingCapacity;
        private BigDecimal quantity = BigDecimal.ZERO;
        private BigDecimal allocatedValue = BigDecimal.ZERO;

        private MutableAllocation(PurchaseCandidate candidate) {
            this.candidate = candidate;
            this.remainingCapacity = candidate.targetGapAmount();
        }

        private void add(BigDecimal additionalQuantity) {
            if (additionalQuantity.compareTo(BigDecimal.ZERO) <= 0) {
                return;
            }
            BigDecimal addedValue = additionalQuantity.multiply(candidate.price());
            quantity = quantity.add(additionalQuantity);
            allocatedValue = allocatedValue.add(addedValue);
            remainingCapacity = remainingCapacity.subtract(addedValue);
        }
    }
}
