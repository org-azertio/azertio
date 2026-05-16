import org.azertio.plugins.htmlreport.HtmlReportBuilder;
import org.azertio.plugins.htmlreport.HtmlReportConfigProvider;
import org.azertio.plugins.htmlreport.HtmlReportConfigHelpProvider;

module org.azertio.plugins.htmlreport {

    requires org.myjtools.jexten;
    requires org.azertio.core;
    requires org.myjtools.imconfig;

    provides org.azertio.core.contributors.ReportBuilder  with HtmlReportBuilder;
    provides org.azertio.core.contributors.ConfigProvider with HtmlReportConfigProvider;
    provides org.azertio.core.contributors.HelpProvider   with HtmlReportConfigHelpProvider;

    exports org.azertio.plugins.htmlreport;
    opens   org.azertio.plugins.htmlreport to org.myjtools.jexten;

}