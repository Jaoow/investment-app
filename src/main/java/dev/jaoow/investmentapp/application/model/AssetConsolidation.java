package dev.jaoow.investmentapp.application.model;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
public class AssetConsolidation {

    private final String tickerSymbol;
    private BigDecimal investedAmount = BigDecimal.ZERO;
    private BigDecimal totalQuantity = BigDecimal.ZERO;

    public void addInvestment(BigDecimal amount) {
        this.investedAmount = this.investedAmount.add(amount);
    }

    public void subtractInvestment(BigDecimal amount) {
        this.investedAmount = this.investedAmount.subtract(amount);
    }

    public void addQuantity(BigDecimal quantity) {
        this.totalQuantity = this.totalQuantity.add(quantity);
    }

    public void subtractQuantity(BigDecimal quantity) {
        this.totalQuantity = this.totalQuantity.subtract(quantity);
    }

    public boolean isValid() {
        return this.totalQuantity.compareTo(BigDecimal.ZERO) > 0;
    }
}
