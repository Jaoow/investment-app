package dev.jaoow.investmentapp.application.service.movement;

import dev.jaoow.investmentapp.application.exception.AssetMovementImportException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class AssetMovementTemplateService {

    public byte[] createMovementsTemplate() {
        return createTemplate(
                "Movimentações",
                List.of("Data", "Tipo", "Ticker", "Quantidade", "Preço unitário"),
                List.of("Preencha uma linha por compra ou venda.", "Use COMPRA ou VENDA no campo Tipo.",
                        "Data no formato dd/MM/yyyy. Quantidade e preço devem ser positivos.")
        );
    }

    public byte[] createPositionsTemplate() {
        return createTemplate(
                "Posição atual",
                List.of("Ticker", "Quantidade", "Preço médio"),
                List.of("Preencha uma linha por ativo atualmente em carteira.",
                        "Esta importação substitui os movimentos atuais do portfólio.",
                        "Quantidade e preço médio devem ser positivos.")
        );
    }

    private byte[] createTemplate(String sheetName, List<String> headers, List<String> instructions) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(sheetName);
            Sheet instructionsSheet = workbook.createSheet("Instruções");

            Row header = sheet.createRow(0);
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers.get(i));
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 20 * 256);
            }
            sheet.createFreezePane(0, 1);
            sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, headers.size() - 1));

            if (headers.contains("Tipo")) {
                DataValidationHelper helper = sheet.getDataValidationHelper();
                DataValidationConstraint constraint = helper.createExplicitListConstraint(new String[]{"COMPRA", "VENDA"});
                DataValidation validation = helper.createValidation(constraint, new org.apache.poi.ss.util.CellRangeAddressList(1, 1000, 1, 1));
                validation.setShowErrorBox(true);
                sheet.addValidationData(validation);
            }

            for (int i = 0; i < instructions.size(); i++) {
                instructionsSheet.createRow(i).createCell(0).setCellValue(instructions.get(i));
                instructionsSheet.setColumnWidth(0, 110 * 256);
            }
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException e) {
            throw new AssetMovementImportException("Could not generate the import template.", e);
        }
    }
}
