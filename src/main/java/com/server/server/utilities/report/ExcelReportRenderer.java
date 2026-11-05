package com.server.server.utilities.report;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.server.server.constants.report.ReportRules;

/**
 * Excel report: a navy brand band, title, and summary rows; a frozen, filterable header; numbers and dates as
 * typed cells; statuses and gender in their colors; zebra rows with horizontal rules; a totals row with SUM
 * formulas for money columns; fitted widths; landscape print setup with a page-numbered footer.
 */
public final class ExcelReportRenderer {

    private static final int HEADER_ROW = 4;
    private static final int MIN_WIDTH = 12;
    private static final int MAX_WIDTH = 50;
    private static final Color NAVY = new Color(30, 58, 138);
    private static final Color MUTED = new Color(100, 116, 139);
    private static final Color ZEBRA = new Color(248, 250, 252);
    private static final Color RULE = new Color(226, 232, 240);
    private static final Color CARD = new Color(241, 245, 249);

    private ExcelReportRenderer() {
    }

    public static byte[] render(ReportTable table) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSSFSheet sheet = workbook.createSheet(sheetName(table.title()));
            sheet.setDisplayGridlines(false);
            ExcelStyles styles = new ExcelStyles(workbook);
            int lastColumn = Math.max(table.headers().size(), 1) - 1;
            List<BigDecimal> totals = ReportCells.totals(table);
            title(sheet, 0, ReportRules.BRAND_NAME + "  |  " + ReportRules.BRAND_TAGLINE, styles.brand(), lastColumn, 30);
            title(sheet, 1, table.title(), styles.title(), lastColumn, 24);
            title(sheet, 2, summary(table, totals), styles.meta(), lastColumn, 18);

            XSSFRow header = sheet.createRow(HEADER_ROW);
            header.setHeightInPoints(24);
            for (int c = 0; c < table.headers().size(); c++) {
                XSSFCell cell = header.createCell(c);
                cell.setCellValue(table.headers().get(c));
                cell.setCellStyle(styles.header());
            }
            for (int r = 0; r < table.rows().size(); r++) {
                XSSFRow row = sheet.createRow(HEADER_ROW + 1 + r);
                row.setHeightInPoints(18);
                List<Object> values = table.rows().get(r);
                for (int c = 0; c < values.size(); c++) {
                    write(row.createCell(c), values.get(c), styles, r % 2 == 1);
                }
            }
            if (totals != null) {
                totalsRow(sheet, table, totals, styles);
            }
            for (int c = 0; c < table.headers().size(); c++) {
                int longest = table.headers().get(c).length() + 2;
                for (List<Object> values : table.rows()) {
                    longest = Math.max(longest, ReportCells.isDate(values.get(c)) ? 19 : ReportCells.text(values.get(c)).length());
                }
                sheet.setColumnWidth(c, Math.min(MAX_WIDTH, Math.max(MIN_WIDTH, longest + 3)) * 256);
            }
            sheet.createFreezePane(0, HEADER_ROW + 1);
            if (!table.headers().isEmpty() && !table.rows().isEmpty()) {
                sheet.setAutoFilter(new CellRangeAddress(HEADER_ROW, HEADER_ROW + table.rows().size(), 0, lastColumn));
            }
            sheet.getPrintSetup().setLandscape(true);
            sheet.setFitToPage(true);
            sheet.getPrintSetup().setFitWidth((short) 1);
            sheet.getPrintSetup().setFitHeight((short) 0);
            sheet.setRepeatingRows(new CellRangeAddress(HEADER_ROW, HEADER_ROW, -1, -1));
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

    private static String summary(ReportTable table, List<BigDecimal> totals) {
        StringBuilder text = new StringBuilder(table.rows().size() + (table.rows().size() == 1 ? " record" : " records"));
        if (table.truncated()) {
            text.append(" (report limit reached)");
        }
        if (totals != null) {
            for (int c = 0; c < totals.size(); c++) {
                if (totals.get(c) != null) {
                    text.append("  |  ").append(ReportCells.totalLabel(table.headers().get(c))).append(": ").append(ReportCells.text(totals.get(c)));
                }
            }
        }
        return text.append("  |  Prepared by ").append(table.generatedBy()).append("  |  ")
                .append(table.generatedAt().format(ReportCells.STAMP)).toString();
    }

    /** "Total" and a live SUM of each money column, under the last row. */
    private static void totalsRow(XSSFSheet sheet, ReportTable table, List<BigDecimal> totals, ExcelStyles styles) {
        int first = HEADER_ROW + 2;
        int last = HEADER_ROW + 1 + table.rows().size();
        XSSFRow row = sheet.createRow(last);
        row.setHeightInPoints(20);
        for (int c = 0; c < totals.size(); c++) {
            XSSFCell cell = row.createCell(c);
            if (totals.get(c) != null) {
                String column = CellReference.convertNumToColString(c);
                cell.setCellFormula("SUM(" + column + first + ":" + column + last + ")");
                cell.setCellStyle(styles.total(true));
            } else {
                cell.setCellValue(c == 0 ? "Total" : "");
                cell.setCellStyle(styles.total(false));
            }
        }
    }

    private static void title(XSSFSheet sheet, int rowIndex, String text, CellStyle style, int lastColumn, float height) {
        XSSFRow row = sheet.createRow(rowIndex);
        row.setHeightInPoints(height);
        for (int c = 0; c <= lastColumn; c++) {
            row.createCell(c).setCellStyle(style);
        }
        row.getCell(0).setCellValue(text);
        if (lastColumn > 0) {
            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, lastColumn));
        }
    }

    private static void write(XSSFCell cell, Object value, ExcelStyles styles, boolean zebra) {
        ReportTone tone = ReportTone.of(value);
        if (value instanceof Number number) {
            cell.setCellValue(number.doubleValue());
            cell.setCellStyle(styles.body(ReportCells.isDecimal(value) ? "#,##0.00" : "#,##0", HorizontalAlignment.RIGHT, zebra, tone));
        } else if (value instanceof LocalDateTime time) {
            cell.setCellValue(time);
            cell.setCellStyle(styles.body("d mmm yyyy, hh:mm", HorizontalAlignment.LEFT, zebra, tone));
        } else if (value instanceof LocalDate date) {
            cell.setCellValue(date);
            cell.setCellStyle(styles.body("d mmm yyyy", HorizontalAlignment.LEFT, zebra, tone));
        } else {
            cell.setCellValue(ReportCells.text(value));
            cell.setCellStyle(styles.body(null, tone.highlighted() ? HorizontalAlignment.CENTER : HorizontalAlignment.LEFT, zebra, tone));
        }
    }

    /** Excel sheet names: at most 31 characters, none of \\ / ? * [ ] : */
    private static String sheetName(String title) {
        String name = title.replaceAll("[\\\\/?*\\[\\]:]", " ");
        return name.isBlank() ? "Report" : name.substring(0, Math.min(31, name.length()));
    }

    private static XSSFColor color(Color rgb) {
        return new XSSFColor(new byte[] { (byte) rgb.getRed(), (byte) rgb.getGreen(), (byte) rgb.getBlue() }, null);
    }

    /** Cell styles created once per workbook and reused (Excel limits the number of styles). */
    private static final class ExcelStyles {
        private final XSSFWorkbook workbook;
        private final Map<String, XSSFCellStyle> body = new HashMap<>();
        private final XSSFCellStyle brand;
        private final XSSFCellStyle title;
        private final XSSFCellStyle meta;
        private final XSSFCellStyle header;
        private final XSSFCellStyle totalLabel;
        private final XSSFCellStyle totalMoney;

        ExcelStyles(XSSFWorkbook workbook) {
            this.workbook = workbook;
            brand = titleStyle(11, true, Color.WHITE, NAVY);
            title = titleStyle(16, true, NAVY, null);
            meta = titleStyle(9, false, MUTED, null);
            header = workbook.createCellStyle();
            header.setFont(font(10, true, Color.WHITE));
            fill(header, NAVY);
            header.setVerticalAlignment(VerticalAlignment.CENTER);
            header.setIndention((short) 1);
            totalLabel = total(null);
            totalMoney = total("#,##0.00");
            totalMoney.setAlignment(HorizontalAlignment.RIGHT);
        }

        CellStyle brand() { return brand; }
        CellStyle title() { return title; }
        CellStyle meta() { return meta; }
        CellStyle header() { return header; }
        CellStyle total(boolean money) { return money ? totalMoney : totalLabel; }

        CellStyle body(String format, HorizontalAlignment align, boolean zebra, ReportTone tone) {
            String key = format + "|" + align + "|" + zebra + "|" + tone;
            return body.computeIfAbsent(key, k -> {
                XSSFCellStyle style = workbook.createCellStyle();
                style.setVerticalAlignment(VerticalAlignment.CENTER);
                style.setAlignment(align);
                style.setWrapText(true);
                style.setBorderBottom(BorderStyle.THIN);
                style.setBottomBorderColor(color(RULE));
                if (format != null) {
                    style.setDataFormat(workbook.createDataFormat().getFormat(format));
                }
                if (tone.highlighted()) {
                    style.setFont(font(10, true, tone.ink));
                    fill(style, tone.tint);
                } else if (zebra) {
                    fill(style, ZEBRA);
                }
                return style;
            });
        }

        private XSSFCellStyle total(String format) {
            XSSFCellStyle style = workbook.createCellStyle();
            style.setFont(font(10, true, NAVY));
            fill(style, CARD);
            style.setBorderTop(BorderStyle.MEDIUM);
            style.setTopBorderColor(color(NAVY));
            style.setVerticalAlignment(VerticalAlignment.CENTER);
            if (format != null) {
                style.setDataFormat(workbook.createDataFormat().getFormat(format));
            }
            return style;
        }

        private XSSFCellStyle titleStyle(int size, boolean bold, Color ink, Color background) {
            XSSFCellStyle style = workbook.createCellStyle();
            style.setFont(font(size, bold, ink));
            style.setVerticalAlignment(VerticalAlignment.CENTER);
            style.setIndention((short) 1);
            if (background != null) {
                fill(style, background);
            }
            return style;
        }

        private XSSFFont font(int size, boolean bold, Color ink) {
            XSSFFont font = workbook.createFont();
            font.setFontHeightInPoints((short) size);
            font.setBold(bold);
            font.setColor(color(ink));
            return font;
        }

        private void fill(XSSFCellStyle style, Color background) {
            style.setFillForegroundColor(color(background));
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
    }
}
