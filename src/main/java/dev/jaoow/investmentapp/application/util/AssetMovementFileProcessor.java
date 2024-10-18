package dev.jaoow.investmentapp.application.util;

import dev.jaoow.investmentapp.application.dto.request.AssetMovementRequest;
import dev.jaoow.investmentapp.domain.model.MovementType;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class AssetMovementFileProcessor {

    private static final Logger logger = LoggerFactory.getLogger(AssetMovementFileProcessor.class);

    private static final String DATE_COLUMN = "Data do Negócio";
    private static final String TYPE_COLUMN = "Tipo de Movimentação";
    private static final String TICKER_COLUMN = "Código de Negociação";
    private static final String QUANTITY_COLUMN = "Quantidade";
    private static final String PRICE_COLUMN = "Preço";

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public List<AssetMovementRequest> processExcelFile(MultipartFile file) throws IOException {
        List<AssetMovementRequest> assetMovements = new ArrayList<>();
        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);
            Map<String, Integer> columnIndexes = getColumnIndexes(sheet.getRow(0));

            for (Row row : sheet) {
                if (row.getRowNum() == 0 || isRowEmpty(row)) {
                    continue; // Skip header and empty rows
                }
                try {
                    AssetMovementRequest assetMovement = mapRowToAssetMovement(row, columnIndexes);
                    assetMovements.add(assetMovement);
                } catch (Exception e) {
                    logger.error("Error processing row {}: {}", row.getRowNum(), e.getMessage());
                }
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

    private AssetMovementRequest mapRowToAssetMovement(Row row, Map<String, Integer> columnIndexes) {
        AssetMovementRequest assetMovement = new AssetMovementRequest();
        assetMovement.setDate(parseDate(row, columnIndexes.get(DATE_COLUMN)));
        assetMovement.setType(mapMovementType(row.getCell(columnIndexes.get(TYPE_COLUMN)).getStringCellValue()));
        assetMovement.setTickerSymbol(removeFractionalIndicator(row.getCell(columnIndexes.get(TICKER_COLUMN)).getStringCellValue()));
        assetMovement.setQuantity(parseBigDecimal(row, columnIndexes.get(QUANTITY_COLUMN)));
        assetMovement.setPrice(parseBigDecimal(row, columnIndexes.get(PRICE_COLUMN)));
        return assetMovement;
    }

    private LocalDate parseDate(Row row, int columnIndex) {
        Cell cell = row.getCell(columnIndex);
        if (cell == null || cell.getCellType() != CellType.STRING) {
            throw new IllegalArgumentException("Invalid date format in row " + row.getRowNum());
        }
        return LocalDate.parse(cell.getStringCellValue(), formatter);
    }

    private BigDecimal parseBigDecimal(Row row, int columnIndex) {
        Cell cell = row.getCell(columnIndex);
        if (cell == null || cell.getCellType() != CellType.NUMERIC) {
            throw new IllegalArgumentException("Invalid numeric value in row " + row.getRowNum());
        }
        return BigDecimal.valueOf(cell.getNumericCellValue());
    }

    private MovementType mapMovementType(String type) {
        return switch (type) {
            case "Venda" -> MovementType.SELL;
            case "Compra" -> MovementType.BUY;
            default -> throw new IllegalArgumentException("Tipo de movimentação desconhecido: " + type);
        };
    }

    private String removeFractionalIndicator(String ticker) {
        if (ticker != null && ticker.endsWith("F")) {
            return ticker.substring(0, ticker.length() - 1);
        }
        return ticker;
    }

    private boolean isRowEmpty(Row row) {
        for (Cell cell : row) {
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                return false;
            }
        }
        return true;
    }
}
