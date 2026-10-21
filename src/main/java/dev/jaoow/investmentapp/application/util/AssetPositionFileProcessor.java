package dev.jaoow.investmentapp.application.util;

import dev.jaoow.investmentapp.application.dto.request.AssetPositionRequest;
import dev.jaoow.investmentapp.application.exception.AssetMovementImportException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AssetPositionFileProcessor {

    private static final List<String> REQUIRED_COLUMNS = List.of("Ticker", "Quantidade", "Preço médio");

    public List<AssetPositionRequest> processExcelFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new AssetMovementImportException("The position spreadsheet is empty.", null);
        }
        List<AssetPositionRequest> positions = new ArrayList<>();
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            if (workbook.getNumberOfSheets() == 0 || workbook.getSheetAt(0).getRow(0) == null) {
                throw new AssetMovementImportException("The position spreadsheet has no header row.", null);
            }
            Sheet sheet = workbook.getSheetAt(0);
            Map<String, Integer> columns = columnIndexes(sheet.getRow(0));
            List<String> missing = REQUIRED_COLUMNS.stream().filter(column -> !columns.containsKey(column)).toList();
            if (!missing.isEmpty()) {
                throw new AssetMovementImportException("Missing required spreadsheet columns: " + String.join(", ", missing) + ".", null);
            }
            for (Row row : sheet) {
                if (row.getRowNum() == 0 || isEmpty(row)) continue;
                try {
                    AssetPositionRequest position = new AssetPositionRequest();
                    position.setTickerSymbol(text(row, columns.get("Ticker")).toUpperCase());
                    position.setQuantity(number(row, columns.get("Quantidade")));
                    position.setAveragePrice(number(row, columns.get("Preço médio")));
                    if (position.getTickerSymbol().isBlank()) throw new IllegalArgumentException("Ticker is required.");
                    positions.add(position);
                } catch (RuntimeException e) {
                    throw new AssetMovementImportException("Invalid position data in spreadsheet row " + (row.getRowNum() + 1) + ".", e);
                }
            }
        }
        if (positions.isEmpty()) throw new AssetMovementImportException("The position spreadsheet contains no position rows.", null);
        return positions;
    }

    private Map<String, Integer> columnIndexes(Row row) {
        Map<String, Integer> columns = new HashMap<>();
        for (Cell cell : row) columns.put(cell.getStringCellValue().trim(), cell.getColumnIndex());
        return columns;
    }

    private String text(Row row, int index) {
        Cell cell = row.getCell(index);
        if (cell == null || cell.getCellType() != CellType.STRING) throw new IllegalArgumentException("Text value is required.");
        return cell.getStringCellValue().trim();
    }

    private BigDecimal number(Row row, int index) {
        Cell cell = row.getCell(index);
        if (cell == null || cell.getCellType() != CellType.NUMERIC) throw new IllegalArgumentException("Numeric value is required.");
        BigDecimal value = BigDecimal.valueOf(cell.getNumericCellValue());
        if (value.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("Value must be positive.");
        return value;
    }

    private boolean isEmpty(Row row) {
        for (Cell cell : row) if (cell != null && cell.getCellType() != CellType.BLANK) return false;
        return true;
    }
}
