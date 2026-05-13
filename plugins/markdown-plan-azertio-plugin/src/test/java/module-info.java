module org.azertio.plugins.markdownplan.test {

	requires org.junit.jupiter.api;
	requires org.azertio.core;
	requires org.assertj.core;
	requires org.azertio.plugins.markdownplan;
	requires org.azertio.persistence;
	requires org.myjtools.jexten;
	requires org.myjtools.imconfig;
	requires org.myjtools.jexten.maven.artifact.store;

	opens org.azertio.plugins.markdownplan.test to org.junit.platform.commons;

}