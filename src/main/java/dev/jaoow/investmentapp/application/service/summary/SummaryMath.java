package dev.jaoow.investmentapp.application.service.summary;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class SummaryMath {
    private SummaryMath() {
    }

    static BigDecimal percentageChange(BigDecimal investedAmount, BigDecimal profitOrLoss) {
        return investedAmount.signum() == 0 ? BigDecimal.ZERO
                : profitOrLoss.multiply(BigDecimal.valueOf(100))
                        .divide(investedAmount, 2, RoundingMode.HALF_UP);
    }
}
