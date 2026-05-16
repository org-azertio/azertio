import org.azertio.core.contenttypes.JSONContentType;
import org.azertio.core.contenttypes.TextContentType;
import org.azertio.core.contenttypes.XMLContentType;
import org.azertio.core.contenttypes.YAMLContentType;
import org.azertio.core.contributors.*;
import org.azertio.core.validator.DefaultPlanValidator;
import org.azertio.core.messages.MessageProvider;
import org.azertio.core.persistence.TestPlanRepository;

module org.azertio.core {

	requires com.github.benmanes.caffeine;
	requires org.slf4j;
	requires org.myjtools.jexten;
	requires org.myjtools.imconfig;
	requires static lombok;
	requires java.sql;
	requires com.github.f4b6a3.ulid;
	requires org.myjtools.jexten.plugin;
	requires org.myjtools.mavenfetcher;
	requires org.myjtools.jexten.maven.artifact.store;
	requires org.jspecify;
	requires org.yaml.snakeyaml;
	requires org.hamcrest;
	requires com.google.guice;
	requires com.fasterxml.jackson.databind;
	requires com.fasterxml.jackson.dataformat.yaml;
	requires com.networknt.schema;
	requires java.xml;

	exports org.azertio.core;
	exports org.azertio.core.util;
	exports org.azertio.core.testplan;
	exports org.azertio.core.messages;
	exports org.azertio.core.backend;
	exports org.azertio.core.contributors;
	exports org.azertio.core.expressions;
	exports org.azertio.core.datatypes;
	exports org.azertio.core.assertions;
	exports org.azertio.core.docgen;
	exports org.azertio.core.help;
	exports org.azertio.core.validator;
	exports org.azertio.core.steps;
	exports org.azertio.core.contenttypes;
	exports org.azertio.core.persistence;
	exports org.azertio.core.execution;
	exports org.azertio.core.events;

	opens org.azertio.core to org.myjtools.jexten;
	opens org.azertio.core.messages to org.myjtools.jexten;
	opens org.azertio.core.testplan to org.myjtools.jexten;
	opens org.azertio.core.contributors to org.myjtools.jexten;
	opens org.azertio.core.backend to org.myjtools.jexten;
	opens org.azertio.core.assertions to org.myjtools.jexten;
	opens org.azertio.core.contenttypes to org.myjtools.jexten;
	opens org.azertio.core.persistence to org.myjtools.jexten;
	opens org.azertio.core.execution to org.myjtools.jexten;
	opens org.azertio.core.validator to org.myjtools.jexten;
	opens org.azertio.core.steps to org.myjtools.jexten;
	opens org.azertio.core.help to org.myjtools.jexten;

	uses org.azertio.core.contributors.AIIndexProvider;
	uses HelpProvider;
	uses ContentType;
	uses AssertionFactoryProvider;
	uses DataTypeProvider;
	uses TestPlanRepository;
	uses ConfigProvider;
	uses org.azertio.core.messages.MessageProvider;
	uses SuiteAssembler;
	uses org.azertio.core.contributors.StepProvider;
	uses RepositoryFactory;
	uses TestPlanValidator;
	uses ReportBuilder;
	uses EventObserver;

	provides ContentType with
			JSONContentType,
			TextContentType,
			XMLContentType,
			YAMLContentType;
	provides ConfigProvider with org.azertio.core.AzertioConfig;
	provides DataTypeProvider with org.azertio.core.datatypes.CoreDataTypes;
	provides MessageProvider with
		org.azertio.core.assertions.AssertionMessageProvider,
		org.azertio.core.steps.CoreStepMessageProvider;
	provides AssertionFactoryProvider with org.azertio.core.assertions.CoreAssertionFactories;
	provides TestPlanValidator with DefaultPlanValidator;
	provides StepProvider with org.azertio.core.steps.CoreStepProvider;
	provides HelpProvider with
		org.azertio.core.steps.CoreStepHelpProvider,
		org.azertio.core.steps.CoreConfigHelpProvider;

}