import org.myjtools.openbbt.plugins.pdfreport.PdfReportBuilder;
import org.myjtools.openbbt.plugins.pdfreport.PdfReportConfigProvider;

module org.myjtools.openbbt.plugins.pdfreport {

    requires org.myjtools.jexten;
    requires org.myjtools.openbbt.core;
    requires org.myjtools.imconfig;
    requires org.apache.pdfbox;

    provides org.myjtools.openbbt.core.contributors.ReportBuilder  with PdfReportBuilder;
    provides org.myjtools.openbbt.core.contributors.ConfigProvider  with PdfReportConfigProvider;

    exports org.myjtools.openbbt.plugins.pdfreport;
    opens   org.myjtools.openbbt.plugins.pdfreport to org.myjtools.jexten;

}