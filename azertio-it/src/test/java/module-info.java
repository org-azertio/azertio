import org.azertio.core.contributors.StepProvider;
import org.azertio.core.contributors.SuiteAssembler;
import org.azertio.core.messages.MessageProvider;

module org.azertio.it {
	requires org.azertio.cli;
	requires org.azertio.core;
	requires org.azertio.persistence;
	requires org.myjtools.imconfig;
	requires org.myjtools.jexten;
	requires info.picocli;
	requires org.junit.jupiter.api;
	requires org.assertj.core;

	opens org.azertio.it to org.junit.platform.commons, org.myjtools.jexten, org.azertio.core;

	provides StepProvider    with org.azertio.it.TestValidationStepProvider;
	provides SuiteAssembler  with org.azertio.it.TestTreeSuiteAssembler;
	provides MessageProvider with org.azertio.it.TestValidationStepProviderMessageProvider;
}
