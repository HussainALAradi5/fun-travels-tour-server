package com.server.server.utilities.report;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.List;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.server.server.constants.report.ReportRules;

/**
 * A4 landscape PDF: letterhead on every page, meta line, table header repeated per page, rows kept whole,
 * numbers right-aligned, zebra rows, "End of report" line, and a page-numbered footer.
 */
public final class PdfReportRenderer {

    private static final Color ZEBRA = new Color(243, 246, 252);
    private static final Color GRID = new Color(215, 222, 234);
    private static final Color TEXT = new Color(30, 41, 59);
    private static final int MIN_WEIGHT = 4;
    private static final int MAX_WEIGHT = 30;

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

        Font meta = new Font(Font.HELVETICA, 8, Font.NORMAL, PdfLetterhead.MUTED);
        int count = table.rows().size();
        document.add(new Paragraph("Generated " + table.generatedAt().format(ReportCells.STAMP) + "   |   By " + table.generatedBy()
                + "   |   " + count + (count == 1 ? " record" : " records"), meta));
        document.add(new Paragraph(" ", new Font(Font.HELVETICA, 4)));

        if (table.headers().isEmpty()) {
            document.add(new Paragraph("No columns to show.", meta));
        } else {
            document.add(dataTable(table));
        }
        Font note = new Font(Font.HELVETICA, 7.5f, Font.ITALIC, PdfLetterhead.MUTED);
        String end = table.truncated() ? "Showing the first " + count + " records (report limit)." : "End of report - " + count
                + (count == 1 ? " record" : " records");
        document.add(new Paragraph(end, note));
        document.close();
        return out.toByteArray();
    }

    private static PdfPTable dataTable(ReportTable table) {
        PdfPTable pdf = new PdfPTable(columnWeights(table));
        pdf.setWidthPercentage(100);
        pdf.setHeaderRows(1);
        pdf.setSplitRows(false);
        Font head = new Font(Font.HELVETICA, 8, Font.BOLD, Color.WHITE);
        for (String header : table.headers()) {
            PdfPCell cell = new PdfPCell(new Phrase(header, head));
            cell.setBackgroundColor(PdfLetterhead.NAVY);
            cell.setBorderColor(GRID);
            cell.setPadding(5);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            pdf.addCell(cell);
        }
        Font body = new Font(Font.HELVETICA, 7.5f, Font.NORMAL, TEXT);
        for (int r = 0; r < table.rows().size(); r++) {
            for (Object value : table.rows().get(r)) {
                PdfPCell cell = new PdfPCell(new Phrase(ReportCells.text(value), body));
                cell.setPadding(4.5f);
                cell.setBorderColor(GRID);
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cell.setHorizontalAlignment(ReportCells.isNumeric(value) ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);
                if (r % 2 == 1) {
                    cell.setBackgroundColor(ZEBRA);
                }
                pdf.addCell(cell);
            }
        }
        return pdf;
    }

    /** Relative widths from the longest text in each column, within limits. */
    private static float[] columnWeights(ReportTable table) {
        List<String> headers = table.headers();
        float[] weights = new float[headers.size()];
        for (int c = 0; c < headers.size(); c++) {
            int longest = headers.get(c).length();
            for (List<Object> row : table.rows()) {
                longest = Math.max(longest, ReportCells.text(row.get(c)).length());
            }
            weights[c] = Math.max(MIN_WEIGHT, Math.min(MAX_WEIGHT, longest));
        }
        return weights;
    }
}
