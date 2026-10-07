package dev.jaoow.investmentapp;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.util.*;

public class ExcelReaderTest {

    @Test
    public void readExcel() throws Exception {
        Map<String, Integer> counts = new HashMap<>();
        int rowCount = 0;
        try (FileInputStream fis = new FileInputStream("negociacao-2026-10-06-20-47-12.xlsx");
             Workbook workbook = new XSSFWorkbook(fis)) {
             
             Sheet sheet = workbook.getSheetAt(0);
             for (int i = 1; i < sheet.getPhysicalNumberOfRows(); i++) { // skip header
                 Row row = sheet.getRow(i);
                 if (row == null) continue;
                 String date = getCell(row.getCell(0));
                 if (date.isBlank()) continue;
                 String type = getCell(row.getCell(1));
                 String inst = getCell(row.getCell(4));
                 String ticker = getCell(row.getCell(5));
                 String qty = getCell(row.getCell(6));
                 String price = getCell(row.getCell(7));
                 
                 String key = String.join("|", date, type, inst, ticker, qty, price);
                 counts.put(key, counts.getOrDefault(key, 0) + 1);
                 rowCount++;
             }
        }
        
        System.out.println("Total Rows processed: " + rowCount);
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > 1) {
                System.out.println("DUPLICATE FOUND: " + entry.getKey() + " -> " + entry.getValue() + " times");
            }
        }
    }
    
    private String getCell(Cell cell) {
        if (cell == null) return "";
        return cell.toString().trim();
    }
}
