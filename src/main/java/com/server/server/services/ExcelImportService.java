package com.server.server.services;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ExcelImportService {

    /**
     * Unified File Importer (Supports .xlsx, .xls, and .csv)
     * * @param file The file from the request
     * 
     * @param entitySupplier Constructor of the entity (e.g. User::new)
     * @param mapper         Logic to map a List of Strings (representing columns)
     *                       to Entity fields
     */
    public <T> List<T> importFile(
            MultipartFile file,
            Supplier<T> entitySupplier,
            BiConsumer<T, List<String>> mapper) {

        String fileName = file.getOriginalFilename();
        if (fileName != null && fileName.toLowerCase().endsWith(".csv")) {
            return importCsv(file, entitySupplier, mapper);
        } else {
            return importExcel(file, entitySupplier, mapper);
        }
    }

    /**
     * Handles Excel files using Apache POI
     */
    private <T> List<T> importExcel(
            MultipartFile file,
            Supplier<T> entitySupplier,
            BiConsumer<T, List<String>> mapper) {

        try (InputStream is = file.getInputStream(); Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            List<T> entities = new ArrayList<>();

            // Skip header (index 0)
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row))
                    continue;

                List<String> columnValues = new ArrayList<>();
                // Get all possible cells in the row
                for (int c = 0; c < row.getLastCellNum(); c++) {
                    columnValues.add(getSafeCellValue(row, c));
                }

                T entity = entitySupplier.get();
                mapper.accept(entity, columnValues);
                entities.add(entity);
            }
            return entities;
        } catch (Exception e) {
            throw new RuntimeException("Excel processing failed: " + e.getMessage());
        }
    }

    /**
     * Handles CSV files using BufferedReader
     */
    private <T> List<T> importCsv(
            MultipartFile file,
            Supplier<T> entitySupplier,
            BiConsumer<T, List<String>> mapper) {

        List<T> entities = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            // Read first line (header) and skip it
            String line = reader.readLine();

            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty())
                    continue;

                // Split by comma but ignore commas inside quotes (standard CSV behavior)
                String[] valuesArray = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

                List<String> columnValues = Arrays.stream(valuesArray)
                        .map(v -> v.replaceAll("^\"|\"$", "").trim()) // Clean quotes and whitespace
                        .collect(Collectors.toList());

                T entity = entitySupplier.get();
                mapper.accept(entity, columnValues);
                entities.add(entity);
            }
            return entities;
        } catch (Exception e) {
            throw new RuntimeException("CSV processing failed: " + e.getMessage());
        }
    }

    /**
     * Utility to safely extract values from Excel cells
     */
    public String getSafeCellValue(Row row, int cellIndex) {
        Cell cell = row.getCell(cellIndex);
        if (cell == null)
            return "";

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getDateCellValue().toString();
                }
                yield String.format("%.0f", cell.getNumericCellValue());
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try {
                    yield cell.getStringCellValue();
                } catch (Exception e) {
                    yield String.valueOf(cell.getNumericCellValue());
                }
            }
            default -> "";
        };
    }

    private boolean isRowEmpty(Row row) {
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK)
                return false;
        }
        return true;
    }
}