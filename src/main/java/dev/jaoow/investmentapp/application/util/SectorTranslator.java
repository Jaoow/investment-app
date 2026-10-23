package dev.jaoow.investmentapp.application.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;

/** Translates provider sector names (TradingView/FactSet and Yahoo/Brapi taxonomies) to Portuguese. */
public final class SectorTranslator {

    private static final Map<String, String> SECTORS = Map.ofEntries(
            // TradingView / FactSet
            Map.entry("commercial services", "Serviços comerciais"),
            Map.entry("communications", "Telecomunicações"),
            Map.entry("consumer durables", "Bens de consumo duráveis"),
            Map.entry("consumer non durables", "Bens de consumo não duráveis"),
            Map.entry("consumer services", "Serviços ao consumidor"),
            Map.entry("distribution services", "Distribuição"),
            Map.entry("electronic technology", "Tecnologia eletrônica"),
            Map.entry("energy minerals", "Petróleo, gás e carvão"),
            Map.entry("finance", "Financeiro"),
            Map.entry("government", "Governo"),
            Map.entry("health services", "Serviços de saúde"),
            Map.entry("health technology", "Tecnologia em saúde"),
            Map.entry("industrial services", "Serviços industriais"),
            Map.entry("miscellaneous", "Diversos"),
            Map.entry("non energy minerals", "Mineração e siderurgia"),
            Map.entry("process industries", "Indústria de processos"),
            Map.entry("producer manufacturing", "Bens de capital"),
            Map.entry("retail trade", "Varejo"),
            Map.entry("technology services", "Serviços de tecnologia"),
            Map.entry("transportation", "Transporte"),
            Map.entry("utilities", "Utilidade pública"),
            // Yahoo / Brapi
            Map.entry("basic materials", "Materiais básicos"),
            Map.entry("communication services", "Telecomunicações"),
            Map.entry("consumer cyclical", "Consumo cíclico"),
            Map.entry("consumer defensive", "Consumo não cíclico"),
            Map.entry("energy", "Petróleo, gás e carvão"),
            Map.entry("financial", "Financeiro"),
            Map.entry("financial services", "Financeiro"),
            Map.entry("healthcare", "Saúde"),
            Map.entry("industrials", "Bens industriais"),
            Map.entry("real estate", "Imobiliário"),
            Map.entry("technology", "Tecnologia")
    );

    private SectorTranslator() {
    }

    /** Returns the Portuguese name for a known sector, or the original value when it is unknown or already translated. */
    public static String translate(String sector) {
        if (sector == null || sector.isBlank()) return sector;
        return SECTORS.getOrDefault(normalize(sector), sector.trim());
    }

    private static String normalize(String sector) {
        return Normalizer.normalize(sector, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z]+", " ")
                .trim();
    }
}
