package com.server.server.utilities;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

public final class ExcelImportUtils {
    private ExcelImportUtils() {}

    public static List<Map<String, String>> readRows(InputStream input) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(input)) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null) throw new IllegalArgumentException("The spreadsheet has no header row.");

            DataFormatter formatter = new DataFormatter(Locale.ROOT);
            List<String> headers = new ArrayList<>();
            for (int column = 0; column < headerRow.getLastCellNum(); column++) {
                headers.add(normalizeHeader(formatter.formatCellValue(headerRow.getCell(column))));
            }

            List<Map<String, String>> rows = new ArrayList<>();
            for (int index = headerRow.getRowNum() + 1; index <= sheet.getLastRowNum(); index++) {
                Row row = sheet.getRow(index);
                if (row == null) continue;
                Map<String, String> values = new LinkedHashMap<>();
                boolean populated = false;
                for (int column = 0; column < headers.size(); column++) {
                    String value = formatter.formatCellValue(row.getCell(column)).trim();
                    if (!value.isEmpty()) populated = true;
                    values.put(headers.get(column), value);
                }
                if (populated) {
                    values.put("_row", Integer.toString(index + 1));
                    rows.add(values);
                }
            }
            return rows;
        }
    }

    private static String normalizeHeader(String header) {
        return header.trim().replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT);
    }
}
