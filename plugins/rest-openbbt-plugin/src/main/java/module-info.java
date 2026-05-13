import org.myjtools.openbbt.plugins.rest.RestAIIndexProvider;
import org.myjtools.openbbt.plugins.rest.RestConfigProvider;
import org.myjtools.openbbt.plugins.rest.RestMessageProvider;
import org.myjtools.openbbt.plugins.rest.RestStepProvider;

module org.myjtools.openbbt.plugins.rest {

    requires org.myjtools.jexten;
    requires org.myjtools.openbbt.core;
	requires org.myjtools.imconfig;
	requires java.net.http;

	provides org.myjtools.openbbt.core.contributors.StepProvider with RestStepProvider;
    provides org.myjtools.openbbt.core.contributors.ConfigProvider with RestConfigProvider;
    provides org.myjtools.openbbt.core.messages.MessageProvider with RestMessageProvider;
    provides org.myjtools.openbbt.core.contributors.AIIndexProvider with RestAIIndexProvider;

    exports org.myjtools.openbbt.plugins.rest;
    opens org.myjtools.openbbt.plugins.rest to org.myjtools.jexten;

}