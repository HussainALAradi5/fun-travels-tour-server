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
import com.server.server.enums.user.Gender;

class ReportRenderersTest {

    private static ReportTable sample(int rows) {
        List<List<Object>> data = new ArrayList<>();
        for (int i = 0; i < rows; i++) {
            data.add(List.of("T-" + (100 + i), "Gulf Tour " + i, LocalDate.of(2027, 1, 2), new BigDecimal("120.50"), i,
                    GenericStatus.ACTIVE, i % 2 == 0, LocalDateTime.of(2026, 11, 4, 10, 15)));
        }
        return new ReportTable("Tour Inventory", "System Admin", LocalDateTime.of(2026, 11, 4, 15, 30),
                List.of("Tour Number", "Title", "Start Date", "Total", "Available", "Status", "Transport", "Created"), data, false);
    }

    @Test
    void pdfHasLetterheadMetaPageNumbersAndEveryRow() throws Exception {
        PdfReader reader = new PdfReader(PdfReportRenderer.render(sample(60)));
        int pages = reader.getNumberOfPages();
        assertThat(pages).isGreaterThan(1);
        PdfTextExtractor extractor = new PdfTextExtractor(reader);
        String first = extractor.getTextFromPage(1).replaceAll("\\s+", " ");
        String last = extractor.getTextFromPage(pages).replaceAll("\\s+", " ");
        assertThat(first).contains("Fun Travels Tour", "Tour Inventory", "PREPARED BY", "System Admin", "TOTAL", "7,230.00",
                "Page 1 of " + pages, "120.50", "Active", "2 Jan 2027", "4 Nov 2026, 10:15");
        assertThat(last).contains("T-159", "Total", "End of report - 60 records", "Page " + pages + " of " + pages);
        reader.close();
    }

    @Test
    void excelKeepsHeaderAtRowFiveWithTypedCells() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(ExcelReportRenderer.render(sample(3))))) {
            XSSFSheet sheet = workbook.getSheetAt(0);
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).startsWith("Fun Travels Tour");
            assertThat(sheet.getRow(2).getCell(0).getStringCellValue()).contains("3 records", "Total: 361.50", "Prepared by System Admin");
            assertThat(sheet.getRow(4).getCell(0).getStringCellValue()).isEqualTo("Tour Number");
            assertThat(sheet.getRow(5).getCell(2).getCellType()).isEqualTo(CellType.NUMERIC);
            assertThat(sheet.getRow(5).getCell(2).getCellStyle().getDataFormatString()).isEqualTo("d mmm yyyy");
            assertThat(sheet.getRow(5).getCell(3).getNumericCellValue()).isEqualTo(120.5);
            assertThat(sheet.getRow(5).getCell(3).getCellStyle().getDataFormatString()).isEqualTo("#,##0.00");
            assertThat(sheet.getRow(5).getCell(7).getLocalDateTimeCellValue()).isEqualTo(LocalDateTime.of(2026, 11, 4, 10, 15));
            assertThat(sheet.getRow(5).getCell(6).getStringCellValue()).isEqualTo("Yes");
            assertThat(sheet.getRow(5).getCell(5).getStringCellValue()).isEqualTo("Active");
            assertThat(sheet.getRow(5).getCell(5).getCellStyle().getFont().getBold()).isTrue();
            assertThat(sheet.getRow(8).getCell(0).getStringCellValue()).isEqualTo("Total");
            assertThat(sheet.getRow(8).getCell(3).getCellFormula()).isEqualTo("SUM(D6:D8)");
            assertThat(sheet.getRow(8).getCell(4).getCellType()).isNotEqualTo(CellType.FORMULA);
            assertThat(sheet.getPaneInformation().getHorizontalSplitPosition()).isEqualTo((short) 5);
            assertThat(sheet.getFooter().getRight()).isEqualTo("Page &P of &N");
        }
    }

    @Test
    void valuesReadAsWordsAndStatusesGetTheirColors() {
        assertThat(ReportCells.text(GenericStatus.CANCELLED)).isEqualTo("Cancelled");
        assertThat(ReportCells.words("SUPPORT_AGENT")).isEqualTo("Support Agent");
        assertThat(ReportTone.of(GenericStatus.ACTIVE)).isEqualTo(ReportTone.GOOD);
        assertThat(ReportTone.of(GenericStatus.PENDING)).isEqualTo(ReportTone.WARN);
        assertThat(ReportTone.of(GenericStatus.CANCELLED)).isEqualTo(ReportTone.BAD);
        assertThat(ReportTone.of(Gender.FEMALE)).isEqualTo(ReportTone.FEMALE);
        assertThat(ReportTone.of(false)).isEqualTo(ReportTone.BAD);
        assertThat(ReportTone.of("text").highlighted()).isFalse();
    }

    @Test
    void totalsOnlyForMoneyColumns() {
        List<BigDecimal> totals = ReportCells.totals(sample(4));
        assertThat(totals.get(3)).isEqualByComparingTo("482.00");
        assertThat(totals.get(4)).isNull();
        assertThat(ReportCells.totals(sample(0))).isNull();
        ReportTable prices = new ReportTable("Meals", "x", LocalDateTime.now(), List.of("Meal", "Price"),
                List.of(List.of("Lunch", new BigDecimal("6.50"))), false);
        assertThat(ReportCells.totals(prices)).as("unit prices are not added up").isNull();
    }

    @Test
    void emptyReportsStillRender() {
        assertThat(PdfReportRenderer.render(sample(0))).isNotEmpty();
        assertThat(ExcelReportRenderer.render(sample(0))).isNotEmpty();
    }
}
