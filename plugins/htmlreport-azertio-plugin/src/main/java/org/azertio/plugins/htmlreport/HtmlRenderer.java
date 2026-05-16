package org.azertio.plugins.htmlreport;

import org.azertio.core.execution.TestExecution;
import org.azertio.core.execution.TestExecutionNode;
import org.azertio.core.testplan.DataTable;
import org.azertio.core.testplan.Document;
import org.azertio.core.testplan.TestPlan;
import org.azertio.core.testplan.TestPlanNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

class HtmlRenderer {

    private static final DateTimeFormatter DATE_FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private final StringBuilder sb = new StringBuilder(65536);
    private final String footerText;
    private final String logoPath;

    // Accent-derived colors
    private final String navy;
    private final String suiteBg;
    private final String featAccent;
    private final String featBg;
    private final String tagColor;

    HtmlRenderer(String accentColor, String footerText, String logoPath) {
        String ac = accentColor != null ? normalizeHex(accentColor) : "#21213f";
        this.footerText = footerText;
        this.logoPath   = logoPath;
        navy       = darken(ac, 0.20);
        suiteBg    = ac;
        featAccent = lighten(ac, 0.10);
        featBg     = lighten(ac, 0.87);
        tagColor   = lighten(ac, 0.35);
    }

    // =========================================================
    //  Document sections
    // =========================================================

    void startDocument(String title, String organization, List<String> suiteNames,
                       TestExecution execution, TestPlan plan) {
        String planDate = plan.createdAt()      != null ? DATE_FMT.format(plan.createdAt())      : "-";
        String execDate = execution.executedAt() != null ? DATE_FMT.format(execution.executedAt()) : "-";

        int passed = orZero(execution.testPassedCount());
        int failed = orZero(execution.testFailedCount());
        int error  = orZero(execution.testErrorCount());
        int total  = plan.testCaseCount();

        String overallClass = failed > 0 ? "failed" : error > 0 ? "error" : "passed";
        String overallLabel = failed > 0 ? "FAILED" : error > 0 ? "ERROR" : "PASSED";

        sb.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n")
          .append("<meta charset=\"UTF-8\">\n")
          .append("<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">\n")
          .append("<title>").append(e(title)).append(" — Azertio Report</title>\n")
          .append("<style>\n").append(buildCss()).append("</style>\n")
          .append("</head>\n<body>\n<div class=\"report\">\n");

        // Header band
        sb.append("<header class=\"cover\">\n")
          .append("  <div class=\"cover-header\">\n")
          .append("    <span class=\"brand\">Azertio Test Report</span>\n")
          .append(buildLogoHtml())
          .append("  </div>\n")
          .append("  <div class=\"cover-body\">\n")
          .append("    <h1 class=\"report-title\">").append(e(title)).append("</h1>\n");

        if (organization != null && !organization.isBlank()) {
            sb.append("    <p class=\"organization\">").append(e(organization)).append("</p>\n");
        }

        sb.append("    <div class=\"meta-grid\">\n")
          .append("      <span class=\"ml\">Test Plan</span><span class=\"mv\">").append(e(planDate)).append("</span>\n")
          .append("      <span class=\"ml\">Execution</span><span class=\"mv\">").append(e(execDate)).append("</span>\n");

        if (execution.profile() != null && !execution.profile().isBlank()) {
            sb.append("      <span class=\"ml\">Profile</span><span class=\"mv\">")
              .append(e(execution.profile())).append("</span>\n");
        }
        sb.append("      <span class=\"ml\">Test Cases</span><span class=\"mv\">").append(total).append("</span>\n");
        if (!suiteNames.isEmpty()) {
            sb.append("      <span class=\"ml\">Suites</span><span class=\"mv\">")
              .append(e(String.join(", ", suiteNames))).append("</span>\n");
        }
        sb.append("    </div>\n");

        sb.append("    <div class=\"summary-counts\">\n")
          .append("      <span class=\"count-badge ").append(overallClass).append("\">").append(overallLabel).append("</span>\n")
          .append("      <span class=\"count-badge passed\">").append(passed).append(" passed</span>\n")
          .append("      <span class=\"count-badge failed\">").append(failed).append(" failed</span>\n")
          .append("      <span class=\"count-badge error\">").append(error).append(" error</span>\n")
          .append("      <span class=\"count-badge total\">").append(total).append(" total</span>\n")
          .append("    </div>\n")
          .append("  </div>\n")
          .append("</header>\n");
    }

    void addStatistics(int[] suiteCounts, int[] featCounts, int[] tcCounts) {
        sb.append("<section class=\"stats\">\n")
          .append("  <div class=\"section-title\">Statistics</div>\n")
          .append("  <div class=\"charts-row\">\n");
        appendChartSection("Test Suites",   "suite",   suiteCounts);
        appendChartSection("Test Features", "feature", featCounts);
        appendChartSection("Test Cases",    "test",    tcCounts);
        sb.append("  </div>\n</section>\n");
    }

    private void appendChartSection(String label, String unit, int[] counts) {
        int passed = counts[0], failed = counts[1], error = counts[2], total = counts[3];
        sb.append("    <div class=\"chart-col\">\n")
          .append("      <div class=\"chart-label\">").append(label).append("</div>\n")
          .append(buildDonut(passed, failed, error, total, unit))
          .append("      <div class=\"legend\">\n")
          .append(legendRow("PASSED", passed, total, "#2ea84f"))
          .append(legendRow("FAILED", failed, total, "#d72828"))
          .append(legendRow("ERROR",  error,  total, "#e57200"))
          .append("      </div>\n    </div>\n");
    }

    private String legendRow(String label, int count, int total, String color) {
        String pct = total > 0
            ? String.format(Locale.ROOT, "&nbsp;(%.0f%%)", 100.0 * count / total)
            : "";
        return "        <div class=\"legend-row\">"
             + "<span class=\"legend-dot\" style=\"background:" + color + "\"></span>"
             + "<span class=\"legend-label\">" + label + "</span>"
             + "<span class=\"legend-value\">" + count + pct + "</span>"
             + "</div>\n";
    }

    private String buildDonut(int passed, int failed, int error, int total, String unit) {
        if (total <= 0) {
            return "      <svg class=\"donut\" viewBox=\"0 0 36 36\">"
                 + "<circle cx=\"18\" cy=\"18\" r=\"15.9155\" fill=\"transparent\" stroke=\"#e0e0e0\" stroke-width=\"3.5\"/>"
                 + "<text x=\"18\" y=\"20\" text-anchor=\"middle\" style=\"font-size:5px;fill:#888\">0</text>"
                 + "</svg>\n";
        }
        double passedPct = 100.0 * passed / total;
        double failedPct = 100.0 * failed / total;
        double errorPct  = 100.0 * error  / total;
        String units = total == 1 ? unit : unit + "s";

        return "      <svg class=\"donut\" viewBox=\"0 0 36 36\">\n"
             + donutCircle("#e0e0e0", 100, 0, 25)
             + donutCircle("#2ea84f", passedPct, 100 - passedPct, 25)
             + donutCircle("#d72828", failedPct, 100 - failedPct, 25 - passedPct)
             + donutCircle("#e57200", errorPct,  100 - errorPct,  25 - passedPct - failedPct)
             + "<text x=\"18\" y=\"17\" text-anchor=\"middle\" "
             + "style=\"font-size:6px;font-weight:bold;fill:#1a1a1a\">" + total + "</text>\n"
             + "<text x=\"18\" y=\"22\" text-anchor=\"middle\" "
             + "style=\"font-size:3px;fill:#888\">" + e(units) + "</text>\n"
             + "      </svg>\n";
    }

    private String donutCircle(String color, double val, double gap, double offset) {
        if (val <= 0) return "";
        return String.format(Locale.ROOT,
            "        <circle cx=\"18\" cy=\"18\" r=\"15.9155\" fill=\"transparent\" "
            + "stroke=\"%s\" stroke-width=\"3.5\" "
            + "stroke-dasharray=\"%.2f %.2f\" stroke-dashoffset=\"%.2f\"/>\n",
            color, val, gap, offset);
    }

    // =========================================================
    //  Results tree
    // =========================================================

    void startResults() {
        sb.append("<section class=\"results\">\n");
    }

    void endResults() {
        sb.append("</section>\n");
    }

    void startSuite(TestPlanNode node, TestExecutionNode execNode) {
        String name  = node.name() != null ? node.name() : "";
        String badge = resultBadge(execNode);
        sb.append("<div class=\"suite\">\n")
          .append("  <div class=\"suite-header\" onclick=\"toggle(this)\">\n")
          .append("    <span><span class=\"ti\">▾</span><span class=\"suite-name\">")
          .append(e(name)).append("</span></span>\n")
          .append("    ").append(badge).append("\n")
          .append("  </div>\n")
          .append("  <div class=\"suite-body\">\n");
    }

    void endSuite() {
        sb.append("  </div>\n</div>\n");
    }

    void startFeature(TestPlanNode node, TestExecutionNode execNode) {
        String kw    = node.keyword();
        String name  = node.name() != null ? node.name() : "";
        String label = (kw != null && !kw.isBlank()) ? kw.trim() + ": " + name : name;
        String badge = resultBadge(execNode);
        sb.append("    <div class=\"feature\">\n")
          .append("      <div class=\"feature-header\" onclick=\"toggle(this)\">\n")
          .append("        <span><span class=\"ti\">▾</span><span class=\"feature-name\">")
          .append(e(label)).append("</span></span>\n")
          .append("        ").append(badge).append("\n")
          .append("      </div>\n")
          .append("      <div class=\"feature-body\">\n");
    }

    void endFeature() {
        sb.append("      </div>\n    </div>\n");
    }

    void startTestCase(TestPlanNode node, TestExecutionNode execNode) {
        String kw    = node.keyword() != null ? node.keyword().trim() : "Scenario";
        String name  = node.name() != null ? node.name() : "";
        String dur   = duration(execNode);
        String badge = resultBadge(execNode);
        String rc    = resultClass(execNode);

        sb.append("        <div class=\"test-case\" data-result=\"").append(rc).append("\">\n")
          .append("          <div class=\"test-case-header\" onclick=\"toggle(this)\">\n")
          .append("            <div style=\"flex:1;min-width:0\">\n")
          .append("              <div><span class=\"ti\">▾</span>")
          .append("<span class=\"tc-keyword\">").append(e(kw)).append(":</span>")
          .append("<span class=\"tc-name\">").append(e(name)).append("</span></div>\n");

        String id       = node.identifier();
        Set<String> tags = node.tags();
        if ((id != null && !id.isBlank()) || (tags != null && !tags.isEmpty())) {
            sb.append("              <div class=\"tc-meta\">\n");
            if (id != null && !id.isBlank()) {
                sb.append("                <span class=\"tc-id\">#").append(e(id.trim())).append("</span>\n");
            }
            if (tags != null && !tags.isEmpty()) {
                String tagsStr = tags.stream().sorted().map(t -> "@" + t).collect(Collectors.joining("  "));
                sb.append("                <span class=\"tc-tag\">").append(e(tagsStr)).append("</span>\n");
            }
            sb.append("              </div>\n");
        }

        sb.append("            </div>\n")
          .append("            <div class=\"tc-right\">\n");
        if (!dur.isEmpty()) {
            sb.append("              <span class=\"tc-duration\">").append(e(dur)).append("</span>\n");
        }
        sb.append("              ").append(badge).append("\n")
          .append("            </div>\n")
          .append("          </div>\n")
          .append("          <div class=\"test-case-body\">\n");
    }

    void endTestCase() {
        sb.append("          </div>\n        </div>\n");
    }

    void renderStep(TestPlanNode node, TestExecutionNode execNode) {
        String kw   = node.keyword() != null ? node.keyword().trim() : "";
        String name = node.name() != null ? node.name() : "";
        String rc   = resultClass(execNode);
        String rl   = resultLabel(execNode);

        sb.append("            <div class=\"step\">\n")
          .append("              <span class=\"step-kw\">").append(e(kw)).append("</span>\n")
          .append("              <span class=\"step-name\">").append(e(name)).append("</span>\n")
          .append("              <span class=\"step-result ").append(rc).append("\">").append(e(rl)).append("</span>\n")
          .append("            </div>\n");

        if (execNode != null && execNode.message() != null && !execNode.message().isBlank()) {
            sb.append("            <div class=\"step-msg\">").append(e(execNode.message())).append("</div>\n");
        }
        if (node.document()  != null) renderDocument(node.document());
        if (node.dataTable() != null) renderDataTable(node.dataTable());
    }

    void renderVirtualStep(TestPlanNode node) {
        String kw   = node.keyword() != null ? node.keyword().trim() : "";
        String name = node.name() != null ? node.name() : "";
        sb.append("            <div class=\"virtual-step\">")
          .append("<span class=\"step-kw\">").append(e(kw)).append("</span>")
          .append("<span class=\"step-name\">").append(e(name)).append("</span>")
          .append("</div>\n");
    }

    private void renderDocument(Document doc) {
        String content = doc.content() != null ? doc.content() : "";
        sb.append("            <pre class=\"step-doc\">").append(e(content)).append("</pre>\n");
    }

    private void renderDataTable(DataTable table) {
        if (table.values() == null || table.rows() == 0 || table.columns() == 0) return;
        sb.append("            <table class=\"step-table\">\n");
        boolean header = true;
        for (List<String> row : table.values()) {
            sb.append("              <tr>");
            for (String cell : row) {
                if (header) sb.append("<th>").append(e(cell)).append("</th>");
                else        sb.append("<td>").append(e(cell)).append("</td>");
            }
            sb.append("</tr>\n");
            header = false;
        }
        sb.append("            </table>\n");
    }

    void endDocument() {
        sb.append("</div>\n")
          .append("<footer class=\"report-footer\">\n")
          .append("  <span>").append(footerText != null ? e(footerText) : "").append("</span>\n")
          .append("  <span>Generated by <strong>Azertio</strong></span>\n")
          .append("</footer>\n")
          .append("<script>").append(buildJs()).append("</script>\n")
          .append("</body>\n</html>\n");
    }

    String build() {
        return sb.toString();
    }

    // =========================================================
    //  Helpers
    // =========================================================

    private String resultBadge(TestExecutionNode execNode) {
        String rc = resultClass(execNode);
        String rl = resultLabel(execNode);
        if (rl.equals("?")) return "";
        return "<span class=\"badge " + rc + "\">" + rl + "</span>";
    }

    private String resultClass(TestExecutionNode execNode) {
        if (execNode == null || execNode.result() == null) return "unknown";
        return switch (execNode.result()) {
            case PASSED  -> "passed";
            case FAILED  -> "failed";
            case ERROR   -> "error";
            case SKIPPED -> "skipped";
            default      -> "unknown";
        };
    }

    private String resultLabel(TestExecutionNode execNode) {
        if (execNode == null || execNode.result() == null) return "?";
        return execNode.result().name();
    }

    private String duration(TestExecutionNode execNode) {
        if (execNode == null || execNode.startTime() == null || execNode.endTime() == null) return "";
        long ms = execNode.endTime().toEpochMilli() - execNode.startTime().toEpochMilli();
        return ms < 1000 ? ms + "ms" : String.format(Locale.ROOT, "%.3fs", ms / 1000.0);
    }

    private String buildLogoHtml() {
        if (logoPath == null || logoPath.isBlank()) return "";
        try {
            byte[] logoBytes = Files.readAllBytes(Path.of(logoPath));
            String ext = logoPath.toLowerCase().endsWith(".png") ? "png" : "jpeg";
            String b64 = Base64.getEncoder().encodeToString(logoBytes);
            return "    <img class=\"cover-logo\" src=\"data:image/" + ext + ";base64," + b64 + "\" alt=\"Logo\">\n";
        } catch (IOException ignored) {
            return "";
        }
    }

    private static String e(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static int orZero(Integer v) {
        return v != null ? v : 0;
    }

    // =========================================================
    //  Color utilities
    // =========================================================

    private static String normalizeHex(String hex) {
        String h = hex.trim();
        return h.startsWith("#") ? h : "#" + h;
    }

    private static int[] parseHex(String hex) {
        String h = hex.replace("#", "");
        return new int[]{
            Integer.parseInt(h.substring(0, 2), 16),
            Integer.parseInt(h.substring(2, 4), 16),
            Integer.parseInt(h.substring(4, 6), 16)
        };
    }

    private static String toHex(int r, int g, int b) {
        return String.format("#%02x%02x%02x",
            Math.max(0, Math.min(255, r)),
            Math.max(0, Math.min(255, g)),
            Math.max(0, Math.min(255, b)));
    }

    private static String darken(String hex, double amount) {
        int[] c = parseHex(hex);
        return toHex((int)(c[0] * (1 - amount)), (int)(c[1] * (1 - amount)), (int)(c[2] * (1 - amount)));
    }

    private static String lighten(String hex, double amount) {
        int[] c = parseHex(hex);
        return toHex(
            (int)(c[0] + (255 - c[0]) * amount),
            (int)(c[1] + (255 - c[1]) * amount),
            (int)(c[2] + (255 - c[2]) * amount));
    }

    // =========================================================
    //  CSS & JS
    // =========================================================

    private String buildCss() {
        return ":root{"
            + "--navy:" + navy + ";"
            + "--suite-bg:" + suiteBg + ";"
            + "--feat-accent:" + featAccent + ";"
            + "--feat-bg:" + featBg + ";"
            + "--tag-color:" + tagColor + ";"
            + "}\n"
            + STATIC_CSS;
    }

    private static final String STATIC_CSS =
        "*{box-sizing:border-box;margin:0;padding:0}\n"
      + "body{font-family:'Segoe UI',system-ui,-apple-system,sans-serif;background:#f0f0f2;color:#1a1a1a;font-size:14px}\n"
      + ".report{min-height:100vh;display:flex;flex-direction:column}\n"
      + ".cover{background:var(--navy);color:#fff}\n"
      + ".cover-header{padding:14px 32px;display:flex;align-items:center;justify-content:space-between;border-bottom:1px solid rgba(255,255,255,.1)}\n"
      + ".brand{font-size:14px;font-weight:600;color:rgba(255,255,255,.85);letter-spacing:.5px}\n"
      + ".cover-logo{max-height:40px;max-width:120px;object-fit:contain}\n"
      + ".cover-body{max-width:960px;margin:0 auto;padding:40px 32px;width:100%}\n"
      + ".report-title{font-size:26px;font-weight:700;margin-bottom:6px}\n"
      + ".organization{font-size:13px;color:rgba(255,255,255,.6);margin-bottom:20px}\n"
      + ".meta-grid{display:grid;grid-template-columns:110px auto;gap:5px 20px;margin-bottom:20px}\n"
      + ".ml{color:rgba(255,255,255,.5);font-size:12px}\n"
      + ".mv{color:rgba(255,255,255,.9);font-size:12px}\n"
      + ".summary-counts{display:flex;gap:8px;flex-wrap:wrap}\n"
      + ".count-badge{padding:4px 12px;border-radius:4px;font-weight:600;font-size:12px;letter-spacing:.3px}\n"
      + ".count-badge.passed{background:#2ea84f;color:#fff}\n"
      + ".count-badge.failed{background:#d72828;color:#fff}\n"
      + ".count-badge.error{background:#e57200;color:#fff}\n"
      + ".count-badge.total{background:rgba(255,255,255,.1);color:rgba(255,255,255,.7)}\n"
      + ".stats{background:#fff;padding:24px 32px;max-width:960px;margin:16px auto;width:100%;border-radius:6px;box-shadow:0 1px 3px rgba(0,0,0,.1)}\n"
      + ".section-title{font-size:12px;font-weight:700;letter-spacing:.6px;padding:7px 14px;background:var(--suite-bg);color:#fff;border-radius:4px;margin-bottom:20px;text-transform:uppercase}\n"
      + ".charts-row{display:flex;justify-content:space-around;gap:20px;flex-wrap:wrap}\n"
      + ".chart-col{text-align:center;flex:1;min-width:150px}\n"
      + ".chart-label{font-size:13px;font-weight:600;color:#444;margin-bottom:10px}\n"
      + ".donut{width:120px;height:120px}\n"
      + ".legend{margin-top:10px;display:inline-block;text-align:left}\n"
      + ".legend-row{display:flex;align-items:center;gap:6px;margin-bottom:4px;font-size:12px}\n"
      + ".legend-dot{width:10px;height:10px;border-radius:2px;flex-shrink:0}\n"
      + ".legend-label{color:#666;min-width:52px}\n"
      + ".legend-value{color:#1a1a1a;font-weight:600;padding-left:4px}\n"
      + ".results{max-width:960px;margin:0 auto 32px;width:100%}\n"
      + ".suite{margin-top:10px;border-radius:5px;overflow:hidden;box-shadow:0 1px 3px rgba(0,0,0,.1)}\n"
      + ".suite:first-child{margin-top:0}\n"
      + ".suite-header{background:var(--suite-bg);color:#fff;padding:10px 16px;display:flex;align-items:center;justify-content:space-between;cursor:pointer;user-select:none}\n"
      + ".suite-header:hover{filter:brightness(1.1)}\n"
      + ".suite-name{font-size:14px;font-weight:600}\n"
      + ".suite-body{background:#fff}\n"
      + ".feature-header{background:var(--feat-bg);padding:8px 16px;display:flex;align-items:center;justify-content:space-between;cursor:pointer;user-select:none;border-left:3px solid var(--feat-accent);border-bottom:1px solid #e4e4e4}\n"
      + ".feature-header:hover{filter:brightness(.97)}\n"
      + ".feature-name{font-size:13px;font-weight:600;color:#1a1a1a}\n"
      + ".feature-body{background:#fff}\n"
      + ".test-case{border-bottom:1px solid #ebebeb;background:#fff}\n"
      + ".test-case-header{padding:8px 16px 6px 26px;display:flex;align-items:flex-start;justify-content:space-between;cursor:pointer;user-select:none;gap:8px}\n"
      + ".test-case-header:hover{background:#fafafa}\n"
      + ".tc-keyword{color:#aaa;font-size:12px;font-weight:600;white-space:nowrap;margin-right:4px}\n"
      + ".tc-name{font-size:13px;font-weight:600;color:#1a1a1a;word-break:break-word}\n"
      + ".tc-duration{color:#aaa;font-size:11px;white-space:nowrap}\n"
      + ".tc-meta{display:flex;gap:8px;align-items:center;flex-wrap:wrap;margin:2px 0 0 16px}\n"
      + ".tc-id{font-size:11px;font-weight:600;color:#555}\n"
      + ".tc-tag{font-size:11px;color:var(--tag-color)}\n"
      + ".tc-right{display:flex;align-items:center;gap:8px;flex-shrink:0}\n"
      + ".test-case-body{padding:4px 16px 10px 26px}\n"
      + ".step{display:flex;align-items:baseline;padding:2px 0;gap:6px;font-size:12px}\n"
      + ".step-kw{color:#aaa;font-weight:600;white-space:nowrap;min-width:20px}\n"
      + ".step-name{color:#2a2a2a;flex:1;word-break:break-word}\n"
      + ".step-result{font-size:10px;font-weight:700;white-space:nowrap;letter-spacing:.3px}\n"
      + ".step-result.passed{color:#2ea84f}.step-result.failed{color:#d72828}.step-result.error{color:#e57200}\n"
      + ".step-result.skipped,.step-result.unknown{color:#aaa}\n"
      + ".step-msg{font-size:11px;color:#c0392b;padding:5px 10px;background:#fdf3f3;border-left:3px solid #d72828;margin:3px 0 5px 26px;font-family:monospace;white-space:pre-wrap;word-break:break-word;border-radius:0 3px 3px 0}\n"
      + ".step-doc{background:#f7f7f7;border-radius:3px;padding:5px 10px;margin:4px 0 4px 26px;font-family:monospace;font-size:11px;white-space:pre;overflow-x:auto;color:#333;border-left:3px solid #ddd;max-height:180px}\n"
      + ".step-table{margin:4px 0 4px 26px;border-collapse:collapse;font-size:11px}\n"
      + ".step-table th,.step-table td{border:1px solid #ddd;padding:3px 8px;text-align:left}\n"
      + ".step-table th{background:var(--feat-bg);font-weight:600}\n"
      + ".step-table td{font-family:monospace}\n"
      + ".virtual-step{display:flex;align-items:baseline;padding:2px 0;gap:6px;font-size:12px;color:#c8c8c8}\n"
      + ".badge{display:inline-block;padding:2px 8px;border-radius:3px;font-size:11px;font-weight:700;color:#fff;white-space:nowrap;letter-spacing:.3px}\n"
      + ".badge.passed{background:#2ea84f}.badge.failed{background:#d72828}.badge.error{background:#e57200}\n"
      + ".badge.skipped{background:#888}.badge.unknown{background:#aaa}\n"
      + ".ti{font-size:11px;display:inline-block;transition:transform .15s;margin-right:5px}\n"
      + ".suite-header.collapsed .ti,.feature-header.collapsed .ti,.test-case-header.collapsed .ti{transform:rotate(-90deg)}\n"
      + ".report-footer{background:#e8e8eb;border-top:1px solid #d0d0d5;padding:12px 32px;display:flex;justify-content:space-between;align-items:center;font-size:12px;color:#666;margin-top:auto}\n"
      + "@media print{.suite-body,.feature-body,.test-case-body{display:block!important}.ti{display:none}body{background:#fff}.results{box-shadow:none}}\n";

    private static String buildJs() {
        return "function toggle(h){"
            + "h.classList.toggle('collapsed');"
            + "var b=h.nextElementSibling;"
            + "if(b.style.display==='none'){b.style.display='';}else{b.style.display='none';}"
            + "}\n"
            + "document.addEventListener('DOMContentLoaded',function(){"
            + "document.querySelectorAll('.test-case').forEach(function(tc){"
            + "if(tc.dataset.result==='passed'){"
            + "var h=tc.querySelector('.test-case-header');"
            + "var b=tc.querySelector('.test-case-body');"
            + "if(h&&b){h.classList.add('collapsed');b.style.display='none';}"
            + "}});"
            + "});";
    }
}