module org.azertio.plugins.gherkin.test {

	requires org.junit.jupiter.api;
	requires org.azertio.core;
	requires org.assertj.core;
	requires org.azertio.plugins.gherkin;
	requires org.myjtools.gherkinparser;
	requires org.azertio.persistence;
	requires junit5.memory.check;
	requires org.myjtools.jexten;
	requires org.myjtools.imconfig;
	requires org.myjtools.jexten.maven.artifact.store;


	opens org.azertio.plugins.gherkin.test to org.junit.platform.commons;


}