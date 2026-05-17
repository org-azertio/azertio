import org.azertio.plugins.rest.RestAIIndexProvider;
import org.azertio.plugins.rest.RestConfigHelpProvider;
import org.azertio.plugins.rest.RestConfigProvider;
import org.azertio.plugins.rest.RestMessageProvider;
import org.azertio.plugins.rest.RestStepHelpProvider;
import org.azertio.plugins.rest.RestStepProvider;

module org.azertio.plugins.rest {

    requires org.myjtools.jexten;
    requires org.azertio.core;
	requires org.myjtools.imconfig;
	requires java.net.http;

	provides org.azertio.core.contributors.StepProvider with RestStepProvider;
    provides org.azertio.core.contributors.ConfigProvider with RestConfigProvider;
    provides org.azertio.core.messages.MessageProvider with RestMessageProvider;
    provides org.azertio.core.contributors.AIIndexProvider with RestAIIndexProvider;
    provides org.azertio.core.contributors.HelpProvider with RestStepHelpProvider, RestConfigHelpProvider;

    exports org.azertio.plugins.rest;
    opens org.azertio.plugins.rest to org.myjtools.jexten;

}