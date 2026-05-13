package org.azertio.plugins.pdfreport;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.azertio.core.execution.TestExecution;
import org.azertio.core.execution.TestExecutionNode;
import org.azertio.core.testplan.DataTable;
import org.azertio.core.testplan.Document;
import org.azertio.core.testplan.TestPlan;
import org.azertio.core.testplan.TestPlanNode;

import java.io.IOException;
import java.io.OutputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

class PdfRenderer implements AutoCloseable {

    // --- Page layout ---
    private static final float MARGIN    = 40f;
    private static final float PAGE_W    = PDRectangle.A4.getWidth();
    private static final float PAGE_H    = PDRectangle.A4.getHeight();
    private static final float CONTENT_W = PAGE_W - 2 * MARGIN;

    // --- Font sizes ---
    private static final float SZ_COVER  = 18f;
    private static final float SZ_TITLE  = 20f;
    private static final float SZ_META   = 9f;
    private static final float SZ_SUITE  = 12f;
    private static final float SZ_FEAT   = 10f;
    private static final float SZ_CASE   = 9f;
    private static final float SZ_STEP   = 8f;
    private static final float SZ_SMALL  = 7f;

    // --- Line heights ---
    private static final float LH_META  = 14f;
    private static final float LH_SUITE = 18f;
    private static final float LH_FEAT  = 16f;
    private static final float LH_CASE  = 14f;
    private static final float LH_STEP  = 12f;
    private static final float LH_SMALL = 11f;

    // --- Fixed colors (r, g, b  in [0,1]) ---
    private static final float[] WHITE      = {1.00f, 1.00f, 1.00f};
    private static final float[] DARK       = {0.15f, 0.15f, 0.15f};
    private static final float[] GRAY       = {0.55f, 0.55f, 0.55f};
    private static final float[] LIGHT_GRAY = {0.92f, 0.92f, 0.92f};
    private static final float[] PASSED_C   = {0.18f, 0.64f, 0.31f};
    private static final float[] FAILED_C   = {0.84f, 0.16f, 0.16f};
    private static final float[] ERROR_C    = {0.90f, 0.45f, 0.00f};
    private static final float[] SKIPPED_C  = {0.50f, 0.50f, 0.50f};
    private static final float[] VIRTUAL_C  = {0.72f, 0.72f, 0.72f};

    // Default accent: dark navy blue
    private static final float[] DEFAULT_ACCENT = {0.13f, 0.13f, 0.28f};

    // --- Accent-derived colors (set in constructor) ---
    private final float[] NAVY;
    private final float[] SUITE_BG;
    private final float[] FEAT_ACC;
    private final float[] FEAT_BG;
    private final float[] TAG_C;

    private static final DateTimeFormatter DATE_FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    // --- Document state ---
    private final PDDocument doc;
    private final PDFont regular;
    private final PDFont bold;
    private final PDFont mono;
    private final String footerText;
    private PDPageContentStream cs;
    private float y;

    PdfRenderer() throws IOException {
        this(null, null);
    }

    PdfRenderer(float[] accentColor, String footerText) throws IOException {
        doc     = new PDDocument();
        regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        bold    = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        mono    = new PDType1Font(Standard14Fonts.FontName.COURIER);
        this.footerText = footerText;
        float[] a = accentColor != null ? accentColor : DEFAULT_ACCENT;
        NAVY     = darken(a, 0.20f);
        SUITE_BG = a.clone();
        FEAT_ACC = lighten(a, 0.10f);
        FEAT_BG  = lighten(a, 0.87f);
        TAG_C    = lighten(a, 0.45f);
        newPage();
    }

    private static float[] darken(float[] c, float t) {
        return new float[]{c[0] * (1 - t), c[1] * (1 - t), c[2] * (1 - t)};
    }

    private static float[] lighten(float[] c, float t) {
        return new float[]{c[0] + (1 - c[0]) * t, c[1] + (1 - c[1]) * t, c[2] + (1 - c[2]) * t};
    }

    // =========================================================
    //  Page management
    // =========================================================

    private void newPage() throws IOException {
        if (cs != null) cs.close();
        PDPage page = new PDPage(PDRectangle.A4);
        doc.addPage(page);
        cs = new PDPageContentStream(doc, page);
        y = PAGE_H - MARGIN;
    }

    private void ensureSpace(float needed) throws IOException {
        if (y - needed < MARGIN) newPage();
    }

    void breakPage() throws IOException {
        newPage();
    }

    boolean isAtPageTop() {
        return y >= PAGE_H - MARGIN - 5;
    }

    // =========================================================
    //  Low-level drawing primitives
    // =========================================================

    private void fillRect(float x, float rectY, float w, float h, float[] rgb) throws IOException {
        cs.setNonStrokingColor(rgb[0], rgb[1], rgb[2]);
        cs.addRect(x, rectY, w, h);
        cs.fill();
    }

    private void hline(float x1, float lineY, float x2, float[] rgb) throws IOException {
        cs.setStrokingColor(rgb[0], rgb[1], rgb[2]);
        cs.setLineWidth(0.5f);
        cs.moveTo(x1, lineY);
        cs.lineTo(x2, lineY);
        cs.stroke();
    }

    private void text(String t, PDFont font, float size, float x, float textY, float[] rgb) throws IOException {
        t = sanitize(t);
        cs.beginText();
        cs.setNonStrokingColor(rgb[0], rgb[1], rgb[2]);
        cs.setFont(font, size);
        cs.newLineAtOffset(x, textY);
        cs.showText(t);
        cs.endText();
    }

    private void textRight(String t, PDFont font, float size, float rightEdge, float textY, float[] rgb) throws IOException {
        text(t, font, size, rightEdge - tw(t, font, size), textY, rgb);
    }

    // =========================================================
    //  Text utilities
    // =========================================================

    private float tw(String t, PDFont font, float size) {
        try {
            return font.getStringWidth(sanitize(t)) / 1000f * size;
        } catch (Exception e) {
            return t.length() * size * 0.55f;
        }
    }

    private String sanitize(String t) {
        if (t == null) return "";
        StringBuilder sb = new StringBuilder(t.length());
        for (char c : t.toCharArray()) {
            if (c >= 32 && c < 127) sb.append(c);
            else if (c > 127 && c <= 255) sb.append(c); // Latin-1 supplement
            else if (c == '\t') sb.append("    ");
            else sb.append('?');
        }
        return sb.toString();
    }

    private String trunc(String t, PDFont font, float size, float maxW) {
        t = sanitize(t);
        try {
            if (font.getStringWidth(t) / 1000f * size <= maxW) return t;
            float ellW = font.getStringWidth("...") / 1000f * size;
            StringBuilder sb = new StringBuilder();
            float w = 0;
            for (int i = 0; i < t.length(); i++) {
                float cw = font.getStringWidth(String.valueOf(t.charAt(i))) / 1000f * size;
                if (w + cw + ellW > maxW) break;
                sb.append(t.charAt(i));
                w += cw;
            }
            return sb.append("...").toString();
        } catch (Exception e) {
            int max = (int) (maxW / (size * 0.55f));
            return t.length() <= max ? t : t.substring(0, Math.max(0, max - 3)) + "...";
        }
    }

    private List<String> wrap(String t, PDFont font, float size, float maxW) {
        t = sanitize(t);
        List<String> lines = new ArrayList<>();
        if (t.isEmpty()) { lines.add(""); return lines; }
        String[] words = t.split("\\s+");
        StringBuilder line = new StringBuilder();
        float lineW = 0;
        for (String word : words) {
            if (word.isEmpty()) continue;
            String sp = line.isEmpty() ? word : " " + word;
            float ww = tw(sp, font, size);
            if (lineW + ww > maxW && !line.isEmpty()) {
                lines.add(line.toString());
                line = new StringBuilder(word);
                lineW = tw(word, font, size);
            } else {
                line.append(sp);
                lineW += ww;
            }
        }
        if (!line.isEmpty()) lines.add(line.toString());
        return lines.isEmpty() ? List.of(t) : lines;
    }

    // =========================================================
    //  Result helpers
    // =========================================================

    private String resultLabel(TestExecutionNode n) {
        if (n == null || n.result() == null) return "?";
        return n.result().name();
    }

    private float[] resultColor(TestExecutionNode n) {
        if (n == null || n.result() == null) return GRAY;
        return switch (n.result()) {
            case PASSED  -> PASSED_C;
            case FAILED  -> FAILED_C;
            case ERROR   -> ERROR_C;
            case SKIPPED -> SKIPPED_C;
            default      -> GRAY;
        };
    }

    private String duration(TestExecutionNode n) {
        if (n == null || n.startTime() == null || n.endTime() == null) return "";
        long ms = n.endTime().toEpochMilli() - n.startTime().toEpochMilli();
        return ms < 1000 ? ms + "ms" : String.format("%.2fs", ms / 1000.0);
    }

    // =========================================================
    //  Cover page
    // =========================================================

    void addCoverPage(String title, String organization, List<String> suiteNames,
                      TestExecution execution, TestPlan plan, String logoPath) throws IOException {
        // Header band
        fillRect(0, PAGE_H - 64, PAGE_W, 64, NAVY);
        text("Azertio  Test Report", bold, SZ_COVER, MARGIN, PAGE_H - 40, WHITE);

        // Logo (right-aligned inside the header band)
        if (logoPath != null && !logoPath.isBlank()) {
            try {
                PDImageXObject logo = PDImageXObject.createFromFile(logoPath, doc);
                float logoH = 44f;
                float logoW = logoH * logo.getWidth() / (float) logo.getHeight();
                float logoX = PAGE_W - 8f - logoW;
                float logoY = PAGE_H - 64 + 10f;
                cs.drawImage(logo, logoX, logoY, logoW, logoH);
            } catch (Exception ignored) {
                // missing or unreadable logo — silently skip
            }
        }

        // Project title + organization
        y = PAGE_H - 90;
        text(trunc(title, bold, SZ_TITLE, CONTENT_W), bold, SZ_TITLE, MARGIN, y, DARK);
        y -= 24;
        if (organization != null && !organization.isEmpty()) {
            text(organization, regular, SZ_META, MARGIN, y, GRAY);
            y -= LH_META;
        }
        y -= 10;
        hline(MARGIN, y, MARGIN + CONTENT_W, LIGHT_GRAY);
        y -= 16;

        // ---- Metadata (full-width, single column) ----
        float valX = MARGIN + 100;
        String planDateStr = plan.createdAt() != null ? DATE_FMT.format(plan.createdAt()) : "-";
        String execDateStr = execution.executedAt() != null ? DATE_FMT.format(execution.executedAt()) : "-";
        coverMeta("Test Plan Date:", planDateStr,                          valX, y); y -= LH_META;
        coverMeta("Execution Date:", execDateStr,                          valX, y); y -= LH_META;
        if (execution.profile() != null && !execution.profile().isEmpty()) {
            coverMeta("Profile:",       execution.profile(),               valX, y); y -= LH_META;
        }
        coverMeta("Test cases:",    String.valueOf(plan.testCaseCount()),  valX, y); y -= LH_META;

        if (!suiteNames.isEmpty()) {
            y -= 4;
            text("Test suites:", regular, SZ_META, MARGIN, y, GRAY);
            y -= LH_META;
            for (String s : suiteNames) {
                text(trunc("- " + s, regular, SZ_META, CONTENT_W - 8), regular, SZ_META, MARGIN + 8, y, DARK);
                y -= LH_META;
            }
        }

        newPage();
    }

    // =========================================================
    //  Statistics page
    // =========================================================

    // counts: [passed, failed, error, total]
    void addStatisticsPage(int[] suiteCounts, int[] featCounts, int[] tcCounts) throws IOException {
        // Section header bar
        float h = LH_SUITE + 4;
        float rectY = y - LH_SUITE + 4;
        fillRect(MARGIN, rectY, CONTENT_W, LH_SUITE, SUITE_BG);
        text("Statistics", bold, SZ_SUITE, MARGIN + 8, rectY + (LH_SUITE - SZ_SUITE) / 2f, WHITE);
        y -= h + 2;

        float chartR   = 65f;
        float labelH   = SZ_SMALL + 8f;
        float sectionH = labelH + 2 * chartR + 16f + 2 * LH_META;
        float gap      = 28f;
        float totalH   = 3 * sectionH + 2 * gap;
        float cx       = MARGIN + CONTENT_W / 2f;

        // Vertically center the three sections
        float topY = y - (y - MARGIN - totalH) / 2f;

        drawChartSection(suiteCounts, "Test Suites",   "suite",   cx, topY, chartR, labelH);
        topY -= sectionH + gap;
        drawChartSection(featCounts,  "Test Features", "feature", cx, topY, chartR, labelH);
        topY -= sectionH + gap;
        drawChartSection(tcCounts,    "Test Cases",    "test",    cx, topY, chartR, labelH);

        newPage();
    }

    private void drawChartSection(int[] counts, String label, String unit, float cx, float topY,
                                   float r, float labelH) throws IOException {
        // Centered label above chart
        text(label, bold, SZ_SMALL, cx - tw(label, bold, SZ_SMALL) / 2f, topY - SZ_SMALL, DARK);

        // Donut chart
        float chartCY = topY - labelH - r;
        drawDonut(cx, chartCY, r, counts[0], counts[1], counts[2], counts[3], unit);

        // Legend centered below chart
        float legendX = cx - 65f;
        float legendY = chartCY - r - 16f;
        legendEntry("PASSED", counts[0], counts[3], counts[0] > 0 ? PASSED_C : GRAY, legendX, legendY); legendY -= LH_META;
        legendEntry("FAILED", counts[1], counts[3], counts[1] > 0 ? FAILED_C : GRAY, legendX, legendY); legendY -= LH_META;
        legendEntry("ERROR",  counts[2], counts[3], counts[2] > 0 ? ERROR_C  : GRAY, legendX, legendY);
    }

    private void coverMeta(String label, String value, float valueX, float textY) throws IOException {
        text(label, regular, SZ_META, MARGIN, textY, GRAY);
        text(value,  regular, SZ_META, valueX, textY, DARK);
    }

    private void legendEntry(String label, int count, int total, float[] color,
                              float x, float textY) throws IOException {
        float sq = 7f;
        fillRect(x, textY, sq, sq, color);
        text(label, regular, SZ_META, x + sq + 4, textY, GRAY);
        String pct = total > 0
            ? String.format(Locale.ROOT, "%d  (%.0f%%)", count, 100.0 * count / total)
            : "0";
        textRight(pct, bold, SZ_META, x + 130, textY, DARK);
    }

    // ---- Donut chart ----

    private void drawDonut(float cx, float cy, float r,
                            int passed, int failed, int errors, int total,
                            String unit) throws IOException {
        if (total <= 0) return;

        int[]     values = {passed, failed, errors};
        float[][] colors = {PASSED_C, FAILED_C, ERROR_C};

        double angle = Math.PI / 2; // start at 12 o'clock, go counter-clockwise
        for (int i = 0; i < values.length; i++) {
            if (values[i] <= 0) continue;
            double sweep = 2 * Math.PI * values[i] / total;
            drawSlice(cx, cy, r, angle, angle + sweep, colors[i]);
            angle += sweep;
        }

        // White inner circle → donut hole
        drawDisk(cx, cy, r * 0.50f, WHITE);

        // Total count + unit label centered in hole
        String totalStr = String.valueOf(total);
        text(totalStr, bold, 13, cx - tw(totalStr, bold, 13) / 2, cy + 2, DARK);
        String unitLabel = total == 1 ? unit : unit + "s";
        text(unitLabel, regular, SZ_SMALL, cx - tw(unitLabel, regular, SZ_SMALL) / 2, cy - 12, GRAY);
    }

    private void drawSlice(float cx, float cy, float r,
                            double startAngle, double endAngle, float[] color) throws IOException {
        cs.setNonStrokingColor(color[0], color[1], color[2]);
        cs.moveTo(cx, cy);
        double step = Math.PI / 72; // 2.5 degrees → smooth arc
        for (double a = startAngle; a < endAngle; a += step) {
            cs.lineTo((float)(cx + r * Math.cos(a)), (float)(cy + r * Math.sin(a)));
        }
        cs.lineTo((float)(cx + r * Math.cos(endAngle)), (float)(cy + r * Math.sin(endAngle)));
        cs.closePath();
        cs.fill();
    }

    private void drawDisk(float cx, float cy, float r, float[] color) throws IOException {
        cs.setNonStrokingColor(color[0], color[1], color[2]);
        double step = Math.PI / 72;
        cs.moveTo(cx + r, cy);
        for (double a = step; a < 2 * Math.PI; a += step) {
            cs.lineTo((float)(cx + r * Math.cos(a)), (float)(cy + r * Math.sin(a)));
        }
        cs.closePath();
        cs.fill();
    }

    private static int orZero(Integer v) {
        return v != null ? v : 0;
    }

    // =========================================================
    //  Suite header
    // =========================================================

    void renderSuiteHeader(TestPlanNode node, TestExecutionNode execNode) throws IOException {
        float h = LH_SUITE + 4;
        ensureSpace(h + 18);
        y -= 14;
        float rectY = y - LH_SUITE + 4;
        fillRect(MARGIN, rectY, CONTENT_W, LH_SUITE, SUITE_BG);
        float textY = rectY + (LH_SUITE - SZ_SUITE) / 2;
        text(trunc(node.name(), bold, SZ_SUITE, CONTENT_W - 70), bold, SZ_SUITE, MARGIN + 8, textY, WHITE);
        if (execNode != null && execNode.result() != null) {
            // center badge within the bar: baseline = barCenter + SZ_SMALL/2
            badge(resultLabel(execNode), MARGIN + CONTENT_W - 62,
                rectY + LH_SUITE / 2f + SZ_SMALL / 2f, resultColor(execNode));
        }
        y -= h + 2;
    }

    // =========================================================
    //  Feature header
    // =========================================================

    void renderFeatureHeader(TestPlanNode node, TestExecutionNode execNode) throws IOException {
        String label = featureLabel(node);
        float h = LH_FEAT + 4;
        ensureSpace(h + 12);
        y -= 10;
        float rectY = y - LH_FEAT + 4;
        fillRect(MARGIN, rectY, CONTENT_W, LH_FEAT, FEAT_BG);
        fillRect(MARGIN, rectY, 3, LH_FEAT, FEAT_ACC);
        float textY = rectY + (LH_FEAT - SZ_FEAT) / 2;
        text(trunc(label, bold, SZ_FEAT, CONTENT_W - 70), bold, SZ_FEAT, MARGIN + 10, textY, DARK);
        if (execNode != null && execNode.result() != null) {
            badge(resultLabel(execNode), MARGIN + CONTENT_W - 62,
                rectY + LH_FEAT / 2f + SZ_SMALL / 2f, resultColor(execNode));
        }
        y -= h;
    }

    private String featureLabel(TestPlanNode node) {
        String kw = node.keyword();
        String name = node.name() != null ? node.name() : "";
        if (kw != null && !kw.isBlank()) return kw.trim() + ": " + name;
        return name;
    }

    // =========================================================
    //  Test case
    // =========================================================

    void renderTestCase(TestPlanNode node, TestExecutionNode execNode) throws IOException {
        String kw   = node.keyword() != null ? node.keyword().trim() : "Scenario";
        String name = node.name() != null ? node.name() : "";
        float kwW   = tw(kw + ": ", bold, SZ_CASE);
        float textX = MARGIN + 16 + kwW;
        float textMaxW = CONTENT_W - 16 - kwW - 65 - 36;  // 36 reserved for duration
        List<String> lines = wrap(name, bold, SZ_CASE, textMaxW);

        boolean hasMeta = hasTestCaseMeta(node);

        float totalH = lines.size() * LH_CASE + 4 + (hasMeta ? LH_SMALL + 2 : 0);
        ensureSpace(totalH + 16);

        y -= 6;
        hline(MARGIN + 16, y, MARGIN + CONTENT_W, LIGHT_GRAY);
        y -= 8;

        if (hasMeta) {
            float mx       = MARGIN + 16;
            float metaY    = y - SZ_SMALL + 2;
            float maxRight = MARGIN + CONTENT_W - 66;

            String id = node.identifier();
            if (id != null && !id.isBlank()) {
                String idStr = "#" + id.trim();
                text(idStr, bold, SZ_SMALL, mx, metaY, DARK);
                mx += tw(idStr, bold, SZ_SMALL) + 6;
            }

            Set<String> tags = node.tags();
            if (tags != null && !tags.isEmpty()) {
                String tagsStr = tags.stream().sorted()
                    .map(t -> "@" + t).collect(Collectors.joining("  "));
                float available = maxRight - mx;
                if (available > 0) {
                    text(trunc(tagsStr, regular, SZ_SMALL, available), regular, SZ_SMALL,
                        mx, metaY, TAG_C);
                }
            }
            y -= LH_SMALL + 2;
        }

        float firstBaseline = y - SZ_CASE + 2;
        text(kw + ":", bold, SZ_CASE, MARGIN + 16, firstBaseline, GRAY);
        text(lines.get(0), bold, SZ_CASE, textX, firstBaseline, DARK);
        y -= LH_CASE;

        for (int i = 1; i < lines.size(); i++) {
            ensureSpace(LH_CASE);
            text(lines.get(i), bold, SZ_CASE, textX, y - SZ_CASE + 2, DARK);
            y -= LH_CASE;
        }

        // Result badge and duration on the right of the first line
        if (execNode != null && execNode.result() != null) {
            float badgeX        = MARGIN + CONTENT_W - 62;
            float badgeBaseline = firstBaseline + (SZ_CASE + SZ_SMALL) / 2f;
            badge(resultLabel(execNode), badgeX, badgeBaseline, resultColor(execNode));
            String dur = duration(execNode);
            if (!dur.isEmpty()) {
                textRight(dur, regular, SZ_SMALL, badgeX - 6, firstBaseline, GRAY);
            }
        }
        y -= 1;
    }

    private boolean hasTestCaseMeta(TestPlanNode node) {
        String id = node.identifier();
        Set<String> tags = node.tags();
        return (id != null && !id.isBlank()) || (tags != null && !tags.isEmpty());
    }

    private void badge(String label, float x, float baseline, float[] color) throws IOException {
        float padH = 3f;
        float padV = 2f;
        float bw   = tw(label, bold, SZ_SMALL) + padH * 2;
        float bh   = SZ_SMALL + padV * 2;
        fillRect(x, baseline - SZ_SMALL - padV, bw, bh, color);
        text(label, bold, SZ_SMALL, x + padH, baseline - SZ_SMALL, WHITE);
    }

    // =========================================================
    //  Step
    // =========================================================

    void renderStep(TestPlanNode node, TestExecutionNode execNode) throws IOException {
        String kw     = node.keyword() != null ? node.keyword().trim() : "";
        String name   = node.name() != null ? node.name() : "";
        float kwW     = tw(kw + " ", bold, SZ_STEP);
        float indent  = MARGIN + 28;
        float textMaxW = CONTENT_W - 28 - kwW - 50;
        List<String> lines = wrap(name, regular, SZ_STEP, textMaxW);
        float totalH = lines.size() * LH_STEP;
        ensureSpace(totalH);

        float firstBaseline = y - SZ_STEP + 2;
        text(kw, bold, SZ_STEP, indent, firstBaseline, GRAY);
        text(lines.get(0), regular, SZ_STEP, indent + kwW, firstBaseline, DARK);
        y -= LH_STEP;

        for (int i = 1; i < lines.size(); i++) {
            ensureSpace(LH_STEP);
            text(lines.get(i), regular, SZ_STEP, indent + kwW, y - SZ_STEP + 2, DARK);
            y -= LH_STEP;
        }

        // Result indicator on the right of the first line
        if (execNode != null && execNode.result() != null) {
            float[] color = resultColor(execNode);
            float dotY = firstBaseline - SZ_STEP / 2 + 1;
            float dotX = MARGIN + CONTENT_W - 44;
            fillRect(dotX, dotY - 3, 5, 5, color);
            text(resultLabel(execNode), regular, SZ_SMALL, dotX + 8, dotY - SZ_SMALL / 2 + 2, color);
        }

        if (node.document() != null) renderDocument(node.document(), indent);
        if (node.dataTable() != null) renderDataTable(node.dataTable(), indent);
    }

    private void renderDocument(Document doc, float indent) throws IOException {
        String content = doc.content() != null ? doc.content() : "";
        String[] rawLines = content.split("\n", -1);
        int limit = Math.min(rawLines.length, 15);
        float blockW = CONTENT_W - 28 - 50;
        y -= 2;
        for (int i = 0; i < limit; i++) {
            ensureSpace(LH_SMALL);
            fillRect(indent, y - LH_SMALL + 4, blockW, LH_SMALL, LIGHT_GRAY);
            text(trunc(rawLines[i], mono, SZ_SMALL, blockW - 8), mono, SZ_SMALL, indent + 4, y - SZ_SMALL + 2, DARK);
            y -= LH_SMALL;
        }
        if (rawLines.length > 15) {
            ensureSpace(LH_SMALL);
            text("... (" + (rawLines.length - 15) + " more lines)", regular, SZ_SMALL, indent + 4, y - SZ_SMALL + 2, GRAY);
            y -= LH_SMALL;
        }
        y -= 2;
    }

    private void renderDataTable(DataTable table, float indent) throws IOException {
        if (table.values() == null || table.rows() == 0 || table.columns() == 0) return;
        int cols = table.columns();
        float blockW = CONTENT_W - 28 - 50;
        float colW   = blockW / cols;
        y -= 2;
        boolean header = true;
        for (List<String> row : table.values()) {
            ensureSpace(LH_SMALL);
            fillRect(indent, y - LH_SMALL + 4, blockW, LH_SMALL, header ? FEAT_BG : WHITE);
            for (int c = 0; c < cols && c < row.size(); c++) {
                PDFont f = header ? bold : mono;
                text(trunc(row.get(c), f, SZ_SMALL, colW - 6), f, SZ_SMALL, indent + c * colW + 3, y - SZ_SMALL + 2, DARK);
            }
            y -= LH_SMALL;
            header = false;
        }
        y -= 2;
    }

    // =========================================================
    //  Virtual step
    // =========================================================

    void renderVirtualStep(TestPlanNode node) throws IOException {
        String kw   = node.keyword() != null ? node.keyword().trim() : "";
        String name = node.name() != null ? node.name() : "";
        float kwW   = tw(kw + " ", bold, SZ_STEP);
        float indent = MARGIN + 28;
        ensureSpace(LH_STEP);
        float baseline = y - SZ_STEP + 2;
        text(kw, bold, SZ_STEP, indent, baseline, VIRTUAL_C);
        text(trunc(name, regular, SZ_STEP, CONTENT_W - 28 - kwW - 20), regular, SZ_STEP, indent + kwW, baseline, VIRTUAL_C);
        y -= LH_STEP;
    }

    // =========================================================
    //  Finalization
    // =========================================================

    void save(OutputStream out) throws IOException {
        if (cs != null) {
            cs.close();
            cs = null;
        }
        addPageNumbers();
        doc.save(out);
    }

    private void addPageNumbers() throws IOException {
        int total = doc.getNumberOfPages();
        int num   = 0;
        float footerY = MARGIN / 2 - 2;
        for (PDPage page : doc.getPages()) {
            num++;
            try (PDPageContentStream footer = new PDPageContentStream(
                    doc, page, PDPageContentStream.AppendMode.APPEND, true)) {
                footer.setNonStrokingColor(GRAY[0], GRAY[1], GRAY[2]);
                footer.setFont(regular, SZ_SMALL);

                // Footer text — left-aligned
                if (footerText != null && !footerText.isBlank()) {
                    String ft = trunc(footerText, regular, SZ_SMALL, CONTENT_W - 60);
                    footer.beginText();
                    footer.newLineAtOffset(MARGIN, footerY);
                    footer.showText(sanitize(ft));
                    footer.endText();
                }

                // Page number — right-aligned
                String pageLabel = num + " / " + total;
                float pw = tw(pageLabel, regular, SZ_SMALL);
                footer.beginText();
                footer.newLineAtOffset(PAGE_W - MARGIN - pw, footerY);
                footer.showText(pageLabel);
                footer.endText();
            }
        }
    }

    @Override
    public void close() throws IOException {
        if (cs != null) {
            cs.close();
            cs = null;
        }
        doc.close();
    }
}