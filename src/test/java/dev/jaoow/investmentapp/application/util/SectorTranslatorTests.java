package dev.jaoow.investmentapp.application.util;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SectorTranslatorTests {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "Finance|Financeiro",
            "Financial Services|Financeiro",
            "Energy Minerals|Petróleo, gás e carvão",
            "Non-Energy Minerals|Mineração e siderurgia",
            "consumer non-durables|Bens de consumo não duráveis",
            "  UTILITIES  |Utilidade pública",
            "Communication Services|Telecomunicações",
            "Real Estate|Imobiliário"
    })
    void translatesProviderSectorsRegardlessOfCaseAndPunctuation(String sector, String expected) {
        assertEquals(expected, SectorTranslator.translate(sector));
    }

    @Test
    void keepsUnknownOrAlreadyTranslatedSectors() {
        assertEquals("Financeiro", SectorTranslator.translate("Financeiro"));
        assertEquals("Saneamento", SectorTranslator.translate(" Saneamento "));
        assertEquals(" ", SectorTranslator.translate(" "));
        assertNull(SectorTranslator.translate(null));
    }
}
