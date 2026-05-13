module org.azertio.plugins.rest.test {

	requires org.azertio.plugins.rest;

	requires org.junit.jupiter.api;
	requires org.azertio.persistence;
	requires org.azertio.plugins.gherkin;
	requires org.myjtools.imconfig;
	requires org.azertio.core;
	requires azertio.test.support;

	opens org.azertio.plugins.rest.test to org.junit.platform.commons;

}
