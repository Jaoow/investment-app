package dev.jaoow.investmentapp.application.service;

import dev.jaoow.investmentapp.domain.entity.AssetMovement;
import dev.jaoow.investmentapp.domain.model.MovementType;
import dev.jaoow.investmentapp.domain.repository.AssetMovementRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AssetMovementImportService {

    private static final String DATE_COLUMN = "Data do Negócio";
    private static final String TYPE_COLUMN = "Tipo de Movimentação";
    private static final String TICKER_COLUMN = "Código de Negociação";
    private static final String QUANTITY_COLUMN = "Quantidade";
    private static final String PRICE_COLUMN = "Preço";

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public List<AssetMovement> processExcelFile(MultipartFile file) throws IOException {
        List<AssetMovement> assetMovements = new ArrayList<>();
        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);
            Map<String, Integer> columnIndexes = getColumnIndexes(sheet.getRow(0));

            for (Row row : sheet) {
                if (row.getRowNum() == 0) {
                    continue; // Skip header
                }
                AssetMovement assetMovement = mapRowToAssetMovement(row, columnIndexes);
                assetMovements.add(assetMovement);
            }
        }
        return assetMovements;
    }

    private Map<String, Integer> getColumnIndexes(Row headerRow) {
        Map<String, Integer> columnIndexes = new HashMap<>();
        for (Cell cell : headerRow) {
            columnIndexes.put(cell.getStringCellValue(), cell.getColumnIndex());
        }
        return columnIndexes;
    }

    private AssetMovement mapRowToAssetMovement(Row row, Map<String, Integer> columnIndexes) {
        AssetMovement assetMovement = new AssetMovement();
        assetMovement.setDate(LocalDate.parse(row.getCell(columnIndexes.get(DATE_COLUMN)).getStringCellValue(), formatter));
        assetMovement.setType(mapMovementType(row.getCell(columnIndexes.get(TYPE_COLUMN)).getStringCellValue()));
        assetMovement.setTickerSymbol(removeFractionalIndicator(row.getCell(columnIndexes.get(TICKER_COLUMN)).getStringCellValue()));
        assetMovement.setQuantity(BigDecimal.valueOf(row.getCell(columnIndexes.get(QUANTITY_COLUMN)).getNumericCellValue()));
        assetMovement.setPrice(BigDecimal.valueOf(row.getCell(columnIndexes.get(PRICE_COLUMN)).getNumericCellValue()));
        return assetMovement;
    }

    private MovementType mapMovementType(String type) {
        return switch (type) {
            case "Venda" -> MovementType.SELL;
            case "Compra" -> MovementType.BUY;
            default -> throw new IllegalArgumentException("Tipo de movimentação desconhecido: " + type);
        };
    }

    private String removeFractionalIndicator(String ticker) {
        if (ticker.endsWith("F")) {
            return ticker.substring(0, ticker.length() - 1);
        }
        return ticker;
    }
}
