package dev.jaoow.investmentapp.application.util;

import dev.jaoow.investmentapp.application.exception.InvalidTickerException;

import java.util.Locale;

public final class TickerSymbol {
    private TickerSymbol() {
    }

    public static String normalize(String value) {
        if (value == null) {
            throw new InvalidTickerException("Informe o ticker.");
        }
        String symbol = value.trim().toUpperCase(Locale.ROOT);
        if (!symbol.matches("[A-Z0-9]{2,12}")) {
            throw new InvalidTickerException("O ticker deve conter de 2 a 12 letras ou números.");
        }
        return symbol;
    }
}
