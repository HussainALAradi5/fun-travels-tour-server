package com.server.server.utilities.report;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import com.server.server.enums.common.GenericStatus;

class ReportRenderersTest {

    private static ReportTable sample(int rows) {
        List<List<Object>> data = new ArrayList<>();
        for (int i = 0; i < rows; i++) {
            data.add(List.of("T-" + (100 + i), "Gulf Tour " + i, LocalDate.of(2027, 1, 2), new BigDecimal("120.50"), i,
                    GenericStatus.ACTIVE, i % 2 == 0, LocalDateTime.of(2026, 11, 4, 10, 15)));
        }
        return new ReportTable("Tour Inventory", "System Admin", LocalDateTime.of(2026, 11, 4, 15, 30),
                List.of("Tour Number", "Title", "Start Date", "Price", "Available", "Status", "Transport", "Created"), data, false);
    }

    @Test
    void pdfHasLetterheadMetaPageNumbersAndEveryRow() throws Exception {
        PdfReader reader = new PdfReader(PdfReportRenderer.render(sample(60)));
        int pages = reader.getNumberOfPages();
        assertThat(pages).isGreaterThan(1);
        PdfTextExtractor extractor = new PdfTextExtractor(reader);
        String first = extractor.getTextFromPage(1).replaceAll("\\s+", " ");
        String last = extractor.getTextFromPage(pages).replaceAll("\\s+", " ");
        assertThat(first).contains("Fun Travels Tour", "Tour Inventory", "By System Admin", "60 records", "Page 1 of " + pages, "120.50");
        assertThat(last).contains("T-159", "End of report - 60 records", "Page " + pages + " of " + pages);
        reader.close();
    }

    @Test
    void excelKeepsHeaderAtRowFiveWithTypedCells() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(ExcelReportRenderer.render(sample(3))))) {
            XSSFSheet sheet = workbook.getSheetAt(0);
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Fun Travels Tour");
            assertThat(sheet.getRow(4).getCell(0).getStringCellValue()).isEqualTo("Tour Number");
            assertThat(sheet.getRow(5).getCell(2).getCellType()).isEqualTo(CellType.NUMERIC);
            assertThat(sheet.getRow(5).getCell(2).getCellStyle().getDataFormatString()).isEqualTo("yyyy-mm-dd");
            assertThat(sheet.getRow(5).getCell(3).getNumericCellValue()).isEqualTo(120.5);
            assertThat(sheet.getRow(5).getCell(3).getCellStyle().getDataFormatString()).isEqualTo("#,##0.00");
            assertThat(sheet.getRow(5).getCell(7).getLocalDateTimeCellValue()).isEqualTo(LocalDateTime.of(2026, 11, 4, 10, 15));
            assertThat(sheet.getRow(5).getCell(6).getStringCellValue()).isEqualTo("Yes");
            assertThat(sheet.getPaneInformation().getHorizontalSplitPosition()).isEqualTo((short) 5);
            assertThat(sheet.getFooter().getRight()).isEqualTo("Page &P of &N");
        }
    }

    @Test
    void emptyReportsStillRender() {
        assertThat(PdfReportRenderer.render(sample(0))).isNotEmpty();
        assertThat(ExcelReportRenderer.render(sample(0))).isNotEmpty();
    }
}
