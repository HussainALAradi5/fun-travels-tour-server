package com.server.server.services.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/** Reads spreadsheet uploads (.xlsx, .xls, .csv). The first row is always a header. */
@Service
public class ExcelImportService {

    public static final String ROW_NUMBER_KEY = "_row";
    private static final String CSV_EXTENSION = ".csv";
    private static final String CSV_SPLIT = ",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)";
    private static final int MAX_ROWS = 5000;

    /** Data rows as positional cell values, header skipped. */
    public List<List<String>> readRows(MultipartFile file) {
        requireFile(file);
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        try (InputStream input = file.getInputStream()) {
            List<List<String>> rows = name.endsWith(CSV_EXTENSION) ? readCsv(input) : readWorkbook(input);
            return rows.isEmpty() ? rows : rows.subList(1, rows.size());
        } catch (IOException e) {
            throw new IllegalArgumentException("The file could not be read: " + e.getMessage());
        }
    }

    /** Data rows keyed by normalized header (letters and digits, lower case) plus {@link #ROW_NUMBER_KEY}. */
    public List<Map<String, String>> readRowsByHeader(InputStream input) throws IOException {
        List<List<String>> rows = readWorkbook(input);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("The spreadsheet has no header row.");
        }
        List<String> headers = rows.get(0).stream().map(ExcelImportService::normalizeHeader).toList();
        List<Map<String, String>> result = new ArrayList<>();
        for (int index = 1; index < rows.size(); index++) {
            Map<String, String> values = new LinkedHashMap<>();
            List<String> row = rows.get(index);
            for (int column = 0; column < headers.size(); column++) {
                values.put(headers.get(column), column < row.size() ? row.get(column) : "");
            }
            values.put(ROW_NUMBER_KEY, Integer.toString(index + 1));
            result.add(values);
        }
        return result;
    }

    private List<List<String>> readWorkbook(InputStream input) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(input)) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter(Locale.ROOT);
            List<List<String>> rows = new ArrayList<>();
            for (int index = sheet.getFirstRowNum(); index <= sheet.getLastRowNum(); index++) {
                Row row = sheet.getRow(index);
                if (row == null) {
                    continue;
                }
                List<String> values = new ArrayList<>();
                for (int column = 0; column < row.getLastCellNum(); column++) {
                    values.add(formatter.formatCellValue(row.getCell(column)).trim());
                }
                addIfPopulated(rows, values);
            }
            return rows;
        }
    }

    private List<List<String>> readCsv(InputStream input) throws IOException {
        List<List<String>> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                addIfPopulated(rows, Arrays.stream(line.split(CSV_SPLIT))
                        .map(value -> value.replaceAll("^\"|\"$", "").trim())
                        .toList());
            }
        }
        return rows;
    }

    private static void addIfPopulated(List<List<String>> rows, List<String> values) {
        if (values.stream().anyMatch(value -> !value.isEmpty())) {
            if (rows.size() > MAX_ROWS) {
                throw new IllegalArgumentException("Import files are limited to " + MAX_ROWS + " rows.");
            }
            rows.add(values);
        }
    }

    private static void requireFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Select a non-empty file.");
        }
    }

    private static String normalizeHeader(String header) {
        return header.trim().replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT);
    }
}
