import org.myjtools.openbbt.core.contenttypes.JSONContentType;
import org.myjtools.openbbt.core.contenttypes.TextContentType;
import org.myjtools.openbbt.core.contenttypes.XMLContentType;
import org.myjtools.openbbt.core.contenttypes.YAMLContentType;
import org.myjtools.openbbt.core.contributors.*;
import org.myjtools.openbbt.core.validator.DefaultPlanValidator;
import org.myjtools.openbbt.core.messages.MessageProvider;
import org.myjtools.openbbt.core.persistence.TestPlanRepository;

module org.myjtools.openbbt.core {

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

	exports org.myjtools.openbbt.core;
	exports org.myjtools.openbbt.core.util;
	exports org.myjtools.openbbt.core.testplan;
	exports org.myjtools.openbbt.core.messages;
	exports org.myjtools.openbbt.core.backend;
	exports org.myjtools.openbbt.core.contributors;
	exports org.myjtools.openbbt.core.expressions;
	exports org.myjtools.openbbt.core.datatypes;
	exports org.myjtools.openbbt.core.assertions;
	exports org.myjtools.openbbt.core.docgen;
	exports org.myjtools.openbbt.core.validator;
	exports org.myjtools.openbbt.core.steps;
	exports org.myjtools.openbbt.core.contenttypes;
	exports org.myjtools.openbbt.core.persistence;
	exports org.myjtools.openbbt.core.execution;
	exports org.myjtools.openbbt.core.events;

	opens org.myjtools.openbbt.core to org.myjtools.jexten;
	opens org.myjtools.openbbt.core.messages to org.myjtools.jexten;
	opens org.myjtools.openbbt.core.testplan to org.myjtools.jexten;
	opens org.myjtools.openbbt.core.contributors to org.myjtools.jexten;
	opens org.myjtools.openbbt.core.backend to org.myjtools.jexten;
	opens org.myjtools.openbbt.core.assertions to org.myjtools.jexten;
	opens org.myjtools.openbbt.core.contenttypes to org.myjtools.jexten;
	opens org.myjtools.openbbt.core.persistence to org.myjtools.jexten;
	opens org.myjtools.openbbt.core.execution to org.myjtools.jexten;
	opens org.myjtools.openbbt.core.validator to org.myjtools.jexten;
	opens org.myjtools.openbbt.core.steps to org.myjtools.jexten;

	uses org.myjtools.openbbt.core.contributors.AIIndexProvider;
	uses ContentType;
	uses AssertionFactoryProvider;
	uses DataTypeProvider;
	uses TestPlanRepository;
	uses ConfigProvider;
	uses org.myjtools.openbbt.core.messages.MessageProvider;
	uses SuiteAssembler;
	uses org.myjtools.openbbt.core.contributors.StepProvider;
	uses RepositoryFactory;
	uses TestPlanValidator;
	uses ReportBuilder;
	uses EventObserver;

	provides ContentType with
			JSONContentType,
			TextContentType,
			XMLContentType,
			YAMLContentType;
	provides ConfigProvider with org.myjtools.openbbt.core.OpenBBTConfig;
	provides DataTypeProvider with org.myjtools.openbbt.core.datatypes.CoreDataTypes;
	provides MessageProvider with
		org.myjtools.openbbt.core.assertions.AssertionMessageProvider,
		org.myjtools.openbbt.core.steps.CoreStepMessageProvider;
	provides AssertionFactoryProvider with org.myjtools.openbbt.core.assertions.CoreAssertionFactories;
	provides TestPlanValidator with DefaultPlanValidator;
	provides StepProvider with org.myjtools.openbbt.core.steps.CoreStepProvider;

}