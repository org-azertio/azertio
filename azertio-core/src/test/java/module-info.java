import org.azertio.core.contributors.StepProvider;
import org.azertio.core.messages.MessageProvider;

module org.azertio.core.test {
	requires org.myjtools.jexten;
	requires org.myjtools.imconfig;
	requires org.azertio.core;
	requires org.junit.jupiter.api;
	requires org.junit.jupiter.params;
	requires org.assertj.core;
	requires com.google.common;
	requires org.hamcrest;
	requires java.xml.crypto;

	opens org.azertio.core.test to org.junit.platform.commons;
	opens org.azertio.core.test.backend to org.junit.platform.commons;
	opens org.azertio.core.test.util to org.junit.platform.commons;
	opens org.azertio.core.test.assertions to org.junit.platform.commons;
	opens org.azertio.core.test.datatypes to org.junit.platform.commons;
	opens org.azertio.core.test.expressions to org.junit.platform.commons;
	opens org.azertio.core.test.docgen to org.junit.platform.commons;
	opens org.azertio.core.test.help to org.junit.platform.commons;
	opens org.azertio.core.test.messages to org.junit.platform.commons;
	opens org.azertio.core.test.execution to org.junit.platform.commons;
	opens org.azertio.core.test.contenttypes to org.junit.platform.commons;

	exports org.azertio.core.test.backend to org.azertio.core, org.myjtools.jexten;

	provides StepProvider with org.azertio.core.test.backend.TestStepProvider;
	provides MessageProvider with org.azertio.core.test.backend.TestStepProviderMessageProvider;

}
