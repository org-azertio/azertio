import org.azertio.core.contributors.SuiteAssembler;

module org.azertio.cli.test {
	requires org.azertio.core;
	requires org.myjtools.imconfig;
	requires info.picocli;
	requires org.junit.jupiter.api;
	requires org.assertj.core;
	requires org.azertio.cli;
	requires org.myjtools.jexten;

	opens org.azertio.cli.test to org.junit.platform.commons, info.picocli, org.myjtools.jexten, org.azertio.core;

	provides SuiteAssembler with org.azertio.cli.test.TestSuiteAssembler;
}
