package dev.jaoow.investmentapp.infrastructure.b3;

import dev.jaoow.investmentapp.application.dto.b3.B3NegotiationItemDto;
import dev.jaoow.investmentapp.application.dto.b3.B3NegotiationSnapshotDto;
import dev.jaoow.investmentapp.application.exception.B3ImportException;
import dev.jaoow.investmentapp.application.service.b3.B3NegotiationFileParser;
import dev.jaoow.investmentapp.domain.model.MovementType;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class B3ExcelNegotiationParser implements B3NegotiationFileParser {

    private static final String XLSX_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String XLSX_EXTENSION = ".xlsx";

    private static final String COL_DATA_NEGOCIO = "Data do Negócio";
    private static final String COL_TIPO_MOVIMENTACAO = "Tipo de Movimentação";
    private static final String COL_INSTITUICAO = "Instituição";
    private static final String COL_CODIGO_NEGOCIACAO = "Código de Negociação";
    private static final String COL_QUANTIDADE = "Quantidade";
    private static final String COL_PRECO = "Preço";
    private static final String COL_VALOR = "Valor";

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public boolean supports(MultipartFile file) {
        if (file == null || file.isEmpty()) return false;
        String contentType = file.getContentType();
        String originalFilename = file.getOriginalFilename();
        return (contentType != null && contentType.equalsIgnoreCase(XLSX_CONTENT_TYPE))
                || (originalFilename != null && originalFilename.toLowerCase().endsWith(XLSX_EXTENSION));
    }

    @Override
    public B3NegotiationSnapshotDto parse(MultipartFile file) {
        validateFile(file);

        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {
            throw new B3ImportException("Failed to read file bytes.", e);
        }
        String fileHash = computeSha256(fileBytes);

        List<B3NegotiationItemDto> items = new ArrayList<>();

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) {

            if (workbook.getNumberOfSheets() == 0) {
                throw new B3ImportException("The spreadsheet file is empty or has no sheets.");
            }

            Sheet sheet = workbook.getSheetAt(0); // Negotiations are usually in the first sheet
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new B3ImportException("The spreadsheet has no header row.");
            }

            Map<String, Integer> columnIndexes = buildColumnIndexMap(headerRow);
            validateRequiredColumns(columnIndexes);

            Map<String, Integer> identicalRowCounter = new HashMap<>();

            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue; // skip header
                if (isDataRowEmpty(row)) continue;
                
                try {
                    B3NegotiationItemDto item = parseRow(row, columnIndexes);
                    if (item != null) {
                        String rawKey = String.format("%s|%s|%s|%s|%s|%s", 
                            item.getDate(), item.getType(), item.getInstitution(), 
                            item.getTickerSymbol(), item.getQuantity(), item.getPrice());
                        
                        int count = identicalRowCounter.getOrDefault(rawKey, 0);
                        item.setHash(item.generateHash(count));
                        identicalRowCounter.put(rawKey, count + 1);
                        
                        items.add(item);
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse row {}: {}", row.getRowNum() + 1, e.getMessage());
                }
            }

        } catch (Exception e) {
            throw new B3ImportException("Failed to read the spreadsheet file. It may be corrupted or invalid.", e);
        }

        if (items.isEmpty()) {
            throw new B3ImportException("No valid negotiation items were found in the spreadsheet.");
        }

        log.info("B3 Excel parser extracted {} negotiation items from '{}'", items.size(), file.getOriginalFilename());

        return B3NegotiationSnapshotDto.builder()
                .fileName(file.getOriginalFilename())
                .fileHash(fileHash)
                .items(items)
                .build();
    }

    private B3NegotiationItemDto parseRow(Row row, Map<String, Integer> columnIndexes) {
        String dateStr = getRequiredString(row, columnIndexes, COL_DATA_NEGOCIO);
        LocalDate date = LocalDate.parse(dateStr, DATE_FORMATTER);

        String typeStr = getRequiredString(row, columnIndexes, COL_TIPO_MOVIMENTACAO);
        MovementType type = typeStr.equalsIgnoreCase("Compra") ? MovementType.BUY : MovementType.SELL;

        String institution = getRequiredString(row, columnIndexes, COL_INSTITUICAO);
        String tickerSymbol = normalizeTickerSymbol(getRequiredString(row, columnIndexes, COL_CODIGO_NEGOCIACAO));

        BigDecimal quantity = parseDecimalCell(row, columnIndexes, COL_QUANTIDADE);
        BigDecimal price = parseDecimalCell(row, columnIndexes, COL_PRECO);
        BigDecimal totalValue = parseDecimalCell(row, columnIndexes, COL_VALOR);

        return B3NegotiationItemDto.builder()
                .date(date)
                .type(type)
                .institution(institution)
                .tickerSymbol(tickerSymbol)
                .quantity(quantity)
                .price(price)
                .totalValue(totalValue)
                .build();
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new B3ImportException("No file was provided or the file is empty.");
        }
        if (!supports(file)) {
            throw new B3ImportException("Unsupported file format. Only Excel (.xlsx) files are accepted.");
        }
    }

    private void validateRequiredColumns(Map<String, Integer> columnIndexes) {
        List<String> required = List.of(COL_DATA_NEGOCIO, COL_TIPO_MOVIMENTACAO, COL_CODIGO_NEGOCIACAO, COL_QUANTIDADE, COL_PRECO);
        List<String> missing = required.stream().filter(col -> !columnIndexes.containsKey(col)).toList();
        if (!missing.isEmpty()) {
            throw new B3ImportException("Missing required columns: " + String.join(", ", missing));
        }
    }

    private Map<String, Integer> buildColumnIndexMap(Row headerRow) {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (Cell cell : headerRow) {
            String value = getCellString(cell);
            if (value != null && !value.isBlank()) {
                map.put(value.trim(), cell.getColumnIndex());
            }
        }
        return map;
    }

    private String getRequiredString(Row row, Map<String, Integer> cols, String columnName) {
        Integer idx = cols.get(columnName);
        if (idx == null) throw new B3ImportException("Required column '" + columnName + "' not found.");
        String value = getCellString(row, idx);
        if (value == null || value.isBlank()) throw new B3ImportException("Required field '" + columnName + "' is empty in row " + (row.getRowNum() + 1));
        return value.trim();
    }

    private BigDecimal parseDecimalCell(Row row, Map<String, Integer> cols, String columnName) {
        Integer idx = cols.get(columnName);
        if (idx == null) return BigDecimal.ZERO;
        Cell cell = row.getCell(idx);
        if (cell == null) return BigDecimal.ZERO;
        if (cell.getCellType() == CellType.STRING) {
            String str = cell.getStringCellValue().trim();
            if (str.isBlank() || str.equals("-")) return BigDecimal.ZERO;
            try {
                return new BigDecimal(str.replace(".", "").replace(",", "."));
            } catch (NumberFormatException e) {
                return BigDecimal.ZERO;
            }
        } else if (cell.getCellType() == CellType.NUMERIC) {
            return BigDecimal.valueOf(cell.getNumericCellValue());
        }
        return BigDecimal.ZERO;
    }

    private String getCellString(Row row, int columnIndex) {
        return getCellString(row.getCell(columnIndex));
    }

    private String getCellString(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                double d = cell.getNumericCellValue();
                if (d == Math.floor(d)) yield String.valueOf((long) d);
                yield String.valueOf(d);
            }
            default -> null;
        };
    }

    private boolean isDataRowEmpty(Row row) {
        for (Cell cell : row) {
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String val = getCellString(cell);
                if (val != null && !val.isBlank()) return false;
            }
        }
        return true;
    }

    private String normalizeTickerSymbol(String ticker) {
        if (ticker != null && ticker.length() > 4 && ticker.endsWith("F")) {
            String withoutF = ticker.substring(0, ticker.length() - 1);
            if (Character.isDigit(withoutF.charAt(withoutF.length() - 1))) {
                return withoutF;
            }
        }
        return ticker;
    }

    private String computeSha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
