package com.server.server.utilities.report;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.server.server.constants.report.ReportRules;

/**
 * Excel report: brand, title, and meta rows; frozen, filterable header; numbers and dates as typed cells;
 * zebra rows; fitted widths; landscape print setup with a page-numbered footer.
 */
public final class ExcelReportRenderer {

    private static final int HEADER_ROW = 4;
    private static final int MIN_WIDTH = 12;
    private static final int MAX_WIDTH = 50;

    private ExcelReportRenderer() {
    }

    public static byte[] render(ReportTable table) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSSFSheet sheet = workbook.createSheet(sheetName(table.title()));
            ExcelStyles styles = new ExcelStyles(workbook);
            int lastColumn = Math.max(table.headers().size(), 1) - 1;
            title(sheet, 0, ReportRules.BRAND_NAME, styles.brand(), lastColumn);
            title(sheet, 1, table.title(), styles.title(), lastColumn);
            title(sheet, 2, "Generated " + table.generatedAt().format(ReportCells.STAMP) + "  |  By " + table.generatedBy()
                    + "  |  " + table.rows().size() + " records" + (table.truncated() ? " (report limit reached)" : ""),
                    styles.meta(), lastColumn);

            XSSFRow header = sheet.createRow(HEADER_ROW);
            header.setHeightInPoints(22);
            for (int c = 0; c < table.headers().size(); c++) {
                XSSFCell cell = header.createCell(c);
                cell.setCellValue(table.headers().get(c));
                cell.setCellStyle(styles.header());
            }
            for (int r = 0; r < table.rows().size(); r++) {
                XSSFRow row = sheet.createRow(HEADER_ROW + 1 + r);
                List<Object> values = table.rows().get(r);
                for (int c = 0; c < values.size(); c++) {
                    write(row.createCell(c), values.get(c), styles, r % 2 == 1);
                }
            }
            for (int c = 0; c < table.headers().size(); c++) {
                int longest = table.headers().get(c).length();
                for (List<Object> values : table.rows()) {
                    longest = Math.max(longest, ReportCells.isDate(values.get(c)) ? 16 : ReportCells.text(values.get(c)).length());
                }
                sheet.setColumnWidth(c, Math.min(MAX_WIDTH, Math.max(MIN_WIDTH, longest + 3)) * 256);
            }
            sheet.createFreezePane(0, HEADER_ROW + 1);
            if (!table.headers().isEmpty()) {
                sheet.setAutoFilter(new CellRangeAddress(HEADER_ROW, HEADER_ROW, 0, lastColumn));
            }
            sheet.getPrintSetup().setLandscape(true);
            sheet.setFitToPage(true);
            sheet.getPrintSetup().setFitWidth((short) 1);
            sheet.getPrintSetup().setFitHeight((short) 0);
            sheet.getFooter().setLeft(ReportRules.CONFIDENTIALITY);
            sheet.getFooter().setCenter(ReportRules.BRAND_NAME);
            sheet.getFooter().setRight("Page &P of &N");
            workbook.getProperties().getCoreProperties().setCreator(table.generatedBy());
            workbook.getProperties().getCoreProperties().setTitle(table.title());
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Could not build the Excel report.", e);
        }
    }

    private static void title(XSSFSheet sheet, int rowIndex, String text, CellStyle style, int lastColumn) {
        XSSFCell cell = sheet.createRow(rowIndex).createCell(0);
        cell.setCellValue(text);
        cell.setCellStyle(style);
        if (lastColumn > 0) {
            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, lastColumn));
        }
    }

    private static void write(XSSFCell cell, Object value, ExcelStyles styles, boolean zebra) {
        if (value instanceof Number number) {
            cell.setCellValue(number.doubleValue());
            cell.setCellStyle(ReportCells.isDecimal(value) ? styles.decimal(zebra) : styles.integer(zebra));
        } else if (value instanceof LocalDateTime time) {
            cell.setCellValue(time);
            cell.setCellStyle(styles.dateTime(zebra));
        } else if (value instanceof LocalDate date) {
            cell.setCellValue(date);
            cell.setCellStyle(styles.date(zebra));
        } else {
            cell.setCellValue(ReportCells.text(value));
            cell.setCellStyle(styles.text(zebra));
        }
    }

    /** Excel sheet names: at most 31 characters, none of \\ / ? * [ ] : */
    private static String sheetName(String title) {
        String name = title.replaceAll("[\\\\/?*\\[\\]:]", " ");
        return name.isBlank() ? "Report" : name.substring(0, Math.min(31, name.length()));
    }

    /** Cell styles created once per workbook (Excel limits the number of styles). */
    private static final class ExcelStyles {
        private final XSSFWorkbook workbook;
        private final XSSFCellStyle[] body = new XSSFCellStyle[10];
        private final XSSFCellStyle brand;
        private final XSSFCellStyle title;
        private final XSSFCellStyle meta;
        private final XSSFCellStyle header;

        ExcelStyles(XSSFWorkbook workbook) {
            this.workbook = workbook;
            brand = titleStyle(16, true, new byte[] { 30, 58, (byte) 138 });
            title = titleStyle(13, true, null);
            meta = titleStyle(9, false, new byte[] { 100, 116, (byte) 139 });
            header = workbook.createCellStyle();
            XSSFFont white = workbook.createFont();
            white.setBold(true);
            white.setColor(new XSSFColor(new byte[] { (byte) 255, (byte) 255, (byte) 255 }, null));
            header.setFont(white);
            header.setFillForegroundColor(new XSSFColor(new byte[] { 30, 58, (byte) 138 }, null));
            header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            header.setVerticalAlignment(VerticalAlignment.CENTER);
            border(header);
        }

        CellStyle brand() { return brand; }
        CellStyle title() { return title; }
        CellStyle meta() { return meta; }
        CellStyle header() { return header; }
        CellStyle text(boolean zebra) { return body(0, zebra, null, HorizontalAlignment.LEFT); }
        CellStyle integer(boolean zebra) { return body(2, zebra, "#,##0", HorizontalAlignment.RIGHT); }
        CellStyle decimal(boolean zebra) { return body(4, zebra, "#,##0.00", HorizontalAlignment.RIGHT); }
        CellStyle date(boolean zebra) { return body(6, zebra, "yyyy-mm-dd", HorizontalAlignment.LEFT); }
        CellStyle dateTime(boolean zebra) { return body(8, zebra, "yyyy-mm-dd hh:mm", HorizontalAlignment.LEFT); }

        private CellStyle body(int slot, boolean zebra, String format, HorizontalAlignment align) {
            int index = slot + (zebra ? 1 : 0);
            if (body[index] == null) {
                XSSFCellStyle style = workbook.createCellStyle();
                style.setWrapText(true);
                style.setVerticalAlignment(VerticalAlignment.TOP);
                style.setAlignment(align);
                border(style);
                if (format != null) {
                    style.setDataFormat(workbook.createDataFormat().getFormat(format));
                }
                if (zebra) {
                    style.setFillForegroundColor(new XSSFColor(new byte[] { (byte) 243, (byte) 246, (byte) 252 }, null));
                    style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                }
                body[index] = style;
            }
            return body[index];
        }

        private XSSFCellStyle titleStyle(int size, boolean bold, byte[] rgb) {
            XSSFCellStyle style = workbook.createCellStyle();
            XSSFFont font = workbook.createFont();
            font.setFontHeightInPoints((short) size);
            font.setBold(bold);
            if (rgb != null) {
                font.setColor(new XSSFColor(rgb, null));
            }
            style.setFont(font);
            return style;
        }

        private void border(XSSFCellStyle style) {
            XSSFColor grid = new XSSFColor(new byte[] { (byte) 215, (byte) 222, (byte) 234 }, null);
            style.setBorderTop(BorderStyle.THIN);
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);
            style.setTopBorderColor(grid);
            style.setBottomBorderColor(grid);
            style.setLeftBorderColor(grid);
            style.setRightBorderColor(grid);
        }
    }
}
