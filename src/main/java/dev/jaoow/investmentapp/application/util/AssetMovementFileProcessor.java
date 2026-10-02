package dev.jaoow.investmentapp.application.util;

import dev.jaoow.investmentapp.application.dto.request.AssetMovementRequest;
import dev.jaoow.investmentapp.application.exception.AssetMovementImportException;
import dev.jaoow.investmentapp.domain.model.MovementType;
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
public class AssetMovementFileProcessor {

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

            if (workbook.getNumberOfSheets() == 0 || workbook.getSheetAt(0).getRow(0) == null) {
                throw new AssetMovementImportException("The spreadsheet is empty or missing its header row.", null);
            }
            Sheet sheet = workbook.getSheetAt(0);
            Map<String, Integer> columnIndexes = getColumnIndexes(sheet.getRow(0));
            validateRequiredColumns(columnIndexes);

            for (Row row : sheet) {
                if (row.getRowNum() == 0 || isRowEmpty(row)) {
                    continue; // Skip header and empty rows
                }
                try {
                    AssetMovementRequest assetMovement = mapRowToAssetMovement(row, columnIndexes);
                    assetMovements.add(assetMovement);
                } catch (RuntimeException e) {
                    throw new AssetMovementImportException(
                            "Invalid movement data in spreadsheet row " + (row.getRowNum() + 1) + ".", e);
                }
            }
        }
        if (assetMovements.isEmpty()) {
            throw new AssetMovementImportException("The spreadsheet contains no movement rows.", null);
        }
        return assetMovements;
    }

    private void validateRequiredColumns(Map<String, Integer> columnIndexes) {
        List<String> requiredColumns = List.of(DATE_COLUMN, TYPE_COLUMN, TICKER_COLUMN, QUANTITY_COLUMN, PRICE_COLUMN);
        List<String> missingColumns = requiredColumns.stream()
                .filter(column -> !columnIndexes.containsKey(column))
                .toList();
        if (!missingColumns.isEmpty()) {
            throw new AssetMovementImportException("Missing required spreadsheet columns: "
                    + String.join(", ", missingColumns) + ".", null);
        }
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
        if (assetMovement.getTickerSymbol() == null || assetMovement.getTickerSymbol().isBlank()) {
            throw new IllegalArgumentException("Ticker is required.");
        }
        if (assetMovement.getQuantity().compareTo(BigDecimal.ZERO) <= 0
                || assetMovement.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity and price must be positive.");
        }
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
