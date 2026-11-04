package com.server.server.utilities.report;

import java.awt.Color;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;
import com.server.server.constants.ReportRules;

/** Draws the navy letterhead and the "Page x of y" footer on every page of a PDF report. */
public class PdfLetterhead extends PdfPageEventHelper {

    static final Color NAVY = new Color(30, 58, 138);
    static final Color ACCENT = new Color(37, 99, 235);
    static final Color MUTED = new Color(100, 116, 139);
    static final float HEADER_HEIGHT = 62f;
    private static final float MARGIN = 28f;

    private final String title;
    private PdfTemplate totalPages;
    private BaseFont regular;

    public PdfLetterhead(String title) {
        this.title = title;
    }

    @Override
    public void onOpenDocument(PdfWriter writer, Document document) {
        totalPages = writer.getDirectContent().createTemplate(40, 12);
        try {
            regular = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
        } catch (Exception e) {
            throw new IllegalStateException("PDF font unavailable", e);
        }
    }

    @Override
    public void onEndPage(PdfWriter writer, Document document) {
        PdfContentByte canvas = writer.getDirectContent();
        Rectangle page = document.getPageSize();
        float top = page.getHeight();
        canvas.saveState();
        canvas.setColorFill(NAVY);
        canvas.rectangle(0, top - HEADER_HEIGHT, page.getWidth(), HEADER_HEIGHT);
        canvas.fill();
        canvas.setColorFill(ACCENT);
        canvas.rectangle(0, top - HEADER_HEIGHT - 3, page.getWidth(), 3);
        canvas.fill();
        canvas.restoreState();

        Font brand = new Font(Font.HELVETICA, 17, Font.BOLD, Color.WHITE);
        Font small = new Font(Font.HELVETICA, 8, Font.NORMAL, Color.WHITE);
        Font heading = new Font(Font.HELVETICA, 13, Font.BOLD, Color.WHITE);
        ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, new Phrase(ReportRules.BRAND_NAME, brand), MARGIN, top - 30, 0);
        ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, new Phrase(ReportRules.BRAND_TAGLINE, small), MARGIN, top - 45, 0);
        ColumnText.showTextAligned(canvas, Element.ALIGN_RIGHT, new Phrase(title, heading), page.getWidth() - MARGIN, top - 30, 0);
        ColumnText.showTextAligned(canvas, Element.ALIGN_RIGHT, new Phrase("Report", small), page.getWidth() - MARGIN, top - 45, 0);

        float footerY = 18;
        canvas.saveState();
        canvas.setColorStroke(MUTED);
        canvas.setLineWidth(0.4f);
        canvas.moveTo(MARGIN, footerY + 12);
        canvas.lineTo(page.getWidth() - MARGIN, footerY + 12);
        canvas.stroke();
        canvas.restoreState();
        Font footer = new Font(Font.HELVETICA, 7, Font.NORMAL, MUTED);
        ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, new Phrase(ReportRules.CONFIDENTIALITY, footer), MARGIN, footerY, 0);
        ColumnText.showTextAligned(canvas, Element.ALIGN_CENTER, new Phrase(ReportRules.BRAND_NAME, footer), page.getWidth() / 2, footerY, 0);
        String label = "Page " + writer.getPageNumber() + " of ";
        float labelWidth = regular.getWidthPoint(label, 7);
        float x = page.getWidth() - MARGIN - labelWidth - 14;
        ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, new Phrase(label, footer), x, footerY, 0);
        canvas.addTemplate(totalPages, x + labelWidth, footerY);
    }

    @Override
    public void onCloseDocument(PdfWriter writer, Document document) {
        totalPages.beginText();
        totalPages.setFontAndSize(regular, 7);
        totalPages.setColorFill(MUTED);
        // On close the writer has already advanced past the last page.
        totalPages.showText(String.valueOf(writer.getPageNumber() - 1));
        totalPages.endText();
    }
}
