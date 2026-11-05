package com.server.server.constants.report;

/** Limits and branding shared by every generated report. */
public final class ReportRules {

    /** Rows are loaded page by page up to this many; larger reports are cut and say so. */
    public static final int MAX_ROWS = 5000;
    public static final int LOAD_PAGE_SIZE = 100;
    public static final String BRAND_NAME = "Fun Travels Tour";
    public static final String BRAND_TAGLINE = "Travel agency management platform";
    public static final String CONFIDENTIALITY = "Confidential - for internal use only";
    public static final String MEDIA_REPORT_FOLDER = "reports";

    private ReportRules() {
    }
}
