import org.azertio.plugins.webui.WebUiAIIndexProvider;
import org.azertio.plugins.webui.WebUiConfigHelpProvider;
import org.azertio.plugins.webui.WebUiConfigProvider;
import org.azertio.plugins.webui.WebUiMessageProvider;
import org.azertio.plugins.webui.WebUiStepHelpProvider;
import org.azertio.plugins.webui.WebUiStepProvider;

module org.azertio.plugins.webui {

    requires org.myjtools.jexten;
    requires org.azertio.core;
    requires org.myjtools.imconfig;
    requires org.seleniumhq.selenium.api;
    requires org.seleniumhq.selenium.support;

    provides org.azertio.core.contributors.StepProvider with WebUiStepProvider;
    provides org.azertio.core.contributors.ConfigProvider with WebUiConfigProvider;
    provides org.azertio.core.messages.MessageProvider with WebUiMessageProvider;
    provides org.azertio.core.contributors.AIIndexProvider with WebUiAIIndexProvider;
    provides org.azertio.core.contributors.HelpProvider with WebUiStepHelpProvider, WebUiConfigHelpProvider;

    exports org.azertio.plugins.webui;
    opens org.azertio.plugins.webui to org.myjtools.jexten;

}