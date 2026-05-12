package org.myjtools.openbbt.plugins.pdfreport;

import org.myjtools.imconfig.Config;
import org.myjtools.jexten.Extension;
import org.myjtools.jexten.Inject;
import org.myjtools.openbbt.core.contributors.ReportBuilder;
import org.myjtools.openbbt.core.persistence.AttachmentRepository;
import org.myjtools.openbbt.core.persistence.TestExecutionRepository;
import org.myjtools.openbbt.core.persistence.TestPlanRepository;
import org.myjtools.openbbt.core.util.Log;

import java.nio.file.Path;
import java.util.UUID;

@Extension(
    name = "PDF Report Builder",
    extensionPointVersion = "1.0"
)
public class PdfReportBuilder implements ReportBuilder {

    private static final Log log = Log.of("plugins.pdfreport");

    @Inject
    Config config;

    @Inject
    TestExecutionRepository executionRepository;

    @Inject
    TestPlanRepository planRepository;

    @Inject
    AttachmentRepository attachmentRepository;

    @Override
    public void buildReport(UUID executionID) {
        Path outputDir = config.get("pdfreport.outputDir", Path::of)
            .orElse(Path.of(".openbbt", "reports"));
        String fileName = "report-" + executionID + ".pdf";
        Path outputFile = outputDir.resolve(fileName);

        log.info("Generating PDF report for execution {} → {}", executionID, outputFile);

        // TODO: implement PDF generation
        // 1. Load execution data:      executionRepository.findExecution(executionID)
        // 2. Load plan node tree:      planRepository.getNodeChildren(planID)
        // 3. Load attachments:         attachmentRepository.findAttachments(executionID)
        // 4. Build PDF with PDFBox:    try (PDDocument doc = new PDDocument()) { ... }
        // 5. Write to outputFile

        throw new UnsupportedOperationException("PDF report generation not yet implemented");
    }

}