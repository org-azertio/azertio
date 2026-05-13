import org.azertio.plugins.pdfreport.PdfReportBuilder;
import org.azertio.plugins.pdfreport.PdfReportConfigProvider;

module org.azertio.plugins.pdfreport {

    requires org.myjtools.jexten;
    requires org.azertio.core;
    requires org.myjtools.imconfig;
    requires org.apache.pdfbox;

    provides org.azertio.core.contributors.ReportBuilder  with PdfReportBuilder;
    provides org.azertio.core.contributors.ConfigProvider  with PdfReportConfigProvider;

    exports org.azertio.plugins.pdfreport;
    opens   org.azertio.plugins.pdfreport to org.myjtools.jexten;

}