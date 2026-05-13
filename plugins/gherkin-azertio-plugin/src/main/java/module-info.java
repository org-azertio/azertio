import org.azertio.plugins.gherkin.GherkinSuiteAssembler;

module org.azertio.plugins.gherkin {

	exports org.azertio.plugins.gherkin;
	
	requires org.myjtools.jexten;
	requires org.azertio.core;
	requires org.myjtools.imconfig;
	requires org.myjtools.gherkinparser;

	provides org.azertio.core.contributors.SuiteAssembler with GherkinSuiteAssembler;

	opens org.azertio.plugins.gherkin to org.myjtools.jexten;

}