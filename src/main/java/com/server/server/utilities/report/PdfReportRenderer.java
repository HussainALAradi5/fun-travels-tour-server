package com.server.server.utilities.report;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.server.server.constants.report.ReportRules;

/**
 * A4 landscape PDF: letterhead on every page; summary cards (records, author, time, money totals); a table with
 * its header repeated per page, rows kept whole, horizontal rules only, numbers right-aligned, statuses and
 * gender in their colors, and a totals row; then an "End of report" line and a page-numbered footer.
 */
public final class PdfReportRenderer {

    private static final Color ZEBRA = new Color(248, 250, 252);
    private static final Color RULE = new Color(226, 232, 240);
    private static final Color CARD = new Color(241, 245, 249);
    private static final Color TEXT = new Color(30, 41, 59);
    private static final float HEAD_SIZE = 7.5f;
    private static final float BODY_SIZE = 7.5f;
    /** Left and right padding of a cell plus a little slack, in points. */
    private static final float CELL_PADDING = 14;
    private static final float MAX_SHARE = 0.3f;
    private static final BaseFont REGULAR = font(BaseFont.HELVETICA);
    private static final BaseFont BOLD = font(BaseFont.HELVETICA_BOLD);
    private static final int MAX_TOTAL_CARDS = 2;

    private PdfReportRenderer() {
    }

    public static byte[] render(ReportTable table) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate(), 28, 28, PdfLetterhead.HEADER_HEIGHT + 18, 44);
        PdfWriter writer = PdfWriter.getInstance(document, out);
        writer.setPageEvent(new PdfLetterhead(table.title()));
        document.addTitle(table.title());
        document.addAuthor(table.generatedBy());
        document.addCreator(ReportRules.BRAND_NAME);
        document.open();

        List<BigDecimal> totals = ReportCells.totals(table);
        document.add(summary(table, totals));
        Font muted = new Font(Font.HELVETICA, 8, Font.NORMAL, PdfLetterhead.MUTED);
        if (table.headers().isEmpty()) {
            document.add(new Paragraph("No columns to show.", muted));
        } else if (table.rows().isEmpty()) {
            document.add(new Paragraph("No records match this report.", muted));
        } else {
            document.add(dataTable(table, totals));
        }
        int count = table.rows().size();
        Font note = new Font(Font.HELVETICA, 7.5f, Font.ITALIC, PdfLetterhead.MUTED);
        Paragraph end = new Paragraph(table.truncated() ? "Showing the first " + records(count) + " (report limit)."
                : "End of report - " + records(count), note);
        end.setSpacingBefore(6);
        document.add(end);
        document.close();
        return out.toByteArray();
    }

    /** One row of soft cards: records, prepared by, generated, and up to two money totals. */
    private static PdfPTable summary(ReportTable table, List<BigDecimal> totals) {
        List<String[]> cards = new ArrayList<>();
        cards.add(new String[] { "Records", String.valueOf(table.rows().size()) + (table.truncated() ? "+" : "") });
        cards.add(new String[] { "Prepared by", table.generatedBy() });
        cards.add(new String[] { "Generated", table.generatedAt().format(ReportCells.STAMP) });
        if (totals != null) {
            for (int c = 0; c < totals.size() && cards.size() < 3 + MAX_TOTAL_CARDS; c++) {
                if (totals.get(c) != null) {
                    cards.add(new String[] { ReportCells.totalLabel(table.headers().get(c)), ReportCells.text(totals.get(c)) });
                }
            }
        }
        PdfPTable strip = new PdfPTable(cards.size());
        strip.setWidthPercentage(100);
        strip.setSpacingAfter(10);
        Font caption = new Font(Font.HELVETICA, 6.5f, Font.BOLD, PdfLetterhead.MUTED);
        Font value = new Font(Font.HELVETICA, 11, Font.BOLD, PdfLetterhead.NAVY);
        for (String[] card : cards) {
            Phrase content = new Phrase();
            content.add(new Chunk(card[0].toUpperCase() + "\n", caption));
            content.add(new Chunk(card[1], value));
            PdfPCell cell = new PdfPCell(content);
            cell.setBackgroundColor(CARD);
            // A blue accent on the left; a white right border leaves a gap between cards.
            cell.setBorder(Rectangle.LEFT | Rectangle.RIGHT);
            cell.setBorderWidthLeft(2.5f);
            cell.setBorderColorLeft(PdfLetterhead.ACCENT);
            cell.setBorderWidthRight(6);
            cell.setBorderColorRight(Color.WHITE);
            cell.setPadding(7);
            cell.setPaddingLeft(9);
            cell.setLeading(0, 1.35f);
            strip.addCell(cell);
        }
        return strip;
    }

    private static PdfPTable dataTable(ReportTable table, List<BigDecimal> totals) {
        PdfPTable pdf = new PdfPTable(columnWeights(table));
        pdf.setWidthPercentage(100);
        pdf.setHeaderRows(1);
        pdf.setSplitRows(false);
        Font head = new Font(Font.HELVETICA, HEAD_SIZE, Font.BOLD, Color.WHITE);
        for (int c = 0; c < table.headers().size(); c++) {
            PdfPCell cell = new PdfPCell(new Phrase(table.headers().get(c).toUpperCase(), head));
            cell.setBackgroundColor(PdfLetterhead.NAVY);
            cell.setBorder(Rectangle.NO_BORDER);
            cell.setPadding(6);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cell.setHorizontalAlignment(numericColumn(table, c) ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);
            pdf.addCell(cell);
        }
        for (int r = 0; r < table.rows().size(); r++) {
            for (Object value : table.rows().get(r)) {
                pdf.addCell(bodyCell(value, r % 2 == 1));
            }
        }
        if (totals != null) {
            Font bold = new Font(Font.HELVETICA, 8, Font.BOLD, PdfLetterhead.NAVY);
            for (int c = 0; c < totals.size(); c++) {
                String text = totals.get(c) != null ? ReportCells.text(totals.get(c)) : c == 0 ? "Total" : "";
                PdfPCell cell = new PdfPCell(new Phrase(text, bold));
                cell.setBorder(Rectangle.TOP);
                cell.setBorderWidthTop(1.2f);
                cell.setBorderColorTop(PdfLetterhead.NAVY);
                cell.setBackgroundColor(CARD);
                cell.setPadding(5.5f);
                cell.setHorizontalAlignment(totals.get(c) != null ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);
                pdf.addCell(cell);
            }
        }
        return pdf;
    }

    /** A body cell: plain text, right-aligned numbers, or a status / gender in its color on a soft tint. */
    private static PdfPCell bodyCell(Object value, boolean zebra) {
        ReportTone tone = ReportTone.of(value);
        Font font = new Font(Font.HELVETICA, BODY_SIZE, tone.highlighted() ? Font.BOLD : Font.NORMAL, tone.highlighted() ? tone.ink : TEXT);
        PdfPCell cell = new PdfPCell(new Phrase(ReportCells.text(value), font));
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColorBottom(RULE);
        cell.setBorderWidthBottom(0.5f);
        cell.setPadding(5);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setHorizontalAlignment(ReportCells.isNumeric(value) ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);
        if (tone.highlighted()) {
            cell.setBackgroundColor(tone.tint);
        } else if (zebra) {
            cell.setBackgroundColor(ZEBRA);
        }
        return cell;
    }

    private static boolean numericColumn(ReportTable table, int column) {
        return !table.rows().isEmpty() && table.rows().stream().allMatch(row -> row.get(column) == null || ReportCells.isNumeric(row.get(column)));
    }

    /**
     * Widths from the widest text in each column, measured in the fonts the table prints in (bold capitals for
     * headings), plus padding; a column never takes more than {@link #MAX_SHARE} of the row, so long text wraps.
     */
    private static float[] columnWeights(ReportTable table) {
        List<String> headers = table.headers();
        float[] weights = new float[headers.size()];
        for (int c = 0; c < headers.size(); c++) {
            float widest = BOLD.getWidthPoint(headers.get(c).toUpperCase(), HEAD_SIZE);
            for (List<Object> row : table.rows()) {
                Object value = row.get(c);
                BaseFont font = ReportTone.of(value).highlighted() ? BOLD : REGULAR;
                widest = Math.max(widest, font.getWidthPoint(ReportCells.text(value), BODY_SIZE));
            }
            weights[c] = Math.min(widest + CELL_PADDING, PageSize.A4.getHeight() * MAX_SHARE);
        }
        return weights;
    }

    private static BaseFont font(String name) {
        try {
            return BaseFont.createFont(name, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
        } catch (Exception e) {
            throw new IllegalStateException("PDF font unavailable", e);
        }
    }

    private static String records(int count) {
        return count + (count == 1 ? " record" : " records");
    }
}
