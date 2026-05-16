import org.azertio.plugins.pdfreport.PdfReportBuilder;
import org.azertio.plugins.pdfreport.PdfReportConfigProvider;
import org.azertio.plugins.pdfreport.PdfReportConfigHelpProvider;

module org.azertio.plugins.pdfreport {

    requires org.myjtools.jexten;
    requires org.azertio.core;
    requires org.myjtools.imconfig;
    requires org.apache.pdfbox;

    provides org.azertio.core.contributors.ReportBuilder  with PdfReportBuilder;
    provides org.azertio.core.contributors.ConfigProvider  with PdfReportConfigProvider;
    provides org.azertio.core.contributors.HelpProvider   with PdfReportConfigHelpProvider;

    exports org.azertio.plugins.pdfreport;
    opens   org.azertio.plugins.pdfreport to org.myjtools.jexten;

}