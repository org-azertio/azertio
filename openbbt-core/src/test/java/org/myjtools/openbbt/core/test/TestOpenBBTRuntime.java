package org.myjtools.openbbt.core.test;

import org.junit.jupiter.api.Test;
import org.myjtools.imconfig.Config;
import org.myjtools.openbbt.core.DataTypes;
import org.myjtools.openbbt.core.OpenBBTConfig;
import org.myjtools.openbbt.core.OpenBBTException;
import org.myjtools.openbbt.core.OpenBBTRuntime;
import org.myjtools.openbbt.core.contributors.DataTypeProvider;
import org.myjtools.openbbt.core.contributors.StepProvider;
import org.myjtools.openbbt.core.events.EventBus;
import org.myjtools.openbbt.core.messages.MessageProvider;
import org.myjtools.openbbt.core.persistence.AttachmentRepository;
import org.myjtools.openbbt.core.persistence.TestExecutionRepository;
import org.myjtools.openbbt.core.persistence.TestPlanRepository;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TestOpenBBTRuntime {

	private static final Config TEST_CONFIG = Config.ofMap(Map.of(
		OpenBBTConfig.RESOURCE_PATH, "src/test/resources",
		OpenBBTConfig.ENV_PATH, "target/.openbbt-runtime",
		"sample.value", "42"
	));

	@Test
	void provideInstancesForConfigReturnsRootAndNamedConfig() {
		var runtime = new OpenBBTRuntime(TEST_CONFIG);

		var rootConfig = runtime.provideInstancesFor(Config.class, null).toList();
		var namedConfig = runtime.provideInstancesFor(Config.class, "sample").toList();

		assertThat(rootConfig).singleElement().isEqualTo(runtime.configuration());
		assertThat(namedConfig).singleElement().satisfies(config ->
			assertThat(((Config) config).getString("value")).contains("42")
		);
	}

	@Test
	void provideInstancesForRepositoryTypesReturnsEmptyWhenNoFactoryExists() {
		var runtime = new OpenBBTRuntime(TEST_CONFIG);

		assertThat(runtime.provideInstancesFor(TestPlanRepository.class, null)).isEmpty();
		assertThat(runtime.provideInstancesFor(TestExecutionRepository.class, null)).isEmpty();
		assertThat(runtime.provideInstancesFor(AttachmentRepository.class, null)).isEmpty();
	}

	@Test
	void provideInstancesForReturnsCoreSingletons() {
		var runtime = new OpenBBTRuntime(TEST_CONFIG);

		assertThat(runtime.provideInstancesFor(DataTypes.class, null))
			.singleElement()
			.satisfies(instance -> assertThat(((DataTypes) instance).byJavaType(Integer.class).name()).isEqualTo("integer"));
		assertThat(runtime.provideInstancesFor(EventBus.class, null)).singleElement().isSameAs(runtime.eventBus());
	}

	@Test
	void repositoryOnlySkipsResourceScanningInfrastructure() {
		var runtime = OpenBBTRuntime.repositoryOnly(TEST_CONFIG);

		assertThat(runtime.resourceSet()).isNull();
		assertThat(runtime.provideInstancesFor(org.myjtools.openbbt.core.ResourceFinder.class, null)).isEmpty();
		assertThat(runtime.provideInstancesFor(org.myjtools.openbbt.core.ResourceSet.class, null)).isEmpty();
		assertThat(runtime.provideInstancesFor(org.myjtools.openbbt.core.ContentTypes.class, null)).isEmpty();
	}

	@Test
	void getContributorsIncludesCoreExtensions() {
		var runtime = new OpenBBTRuntime(TEST_CONFIG);

		assertThat(runtime.getContributedTypes()).contains(
			DataTypeProvider.class,
			MessageProvider.class,
			StepProvider.class
		);
		var contributors = runtime.getContributors();

		// outer key = module name, inner key = type simple name
		var allTypes = contributors.values().stream().flatMap(m -> m.keySet().stream()).toList();
		assertThat(allTypes).contains("DataTypeProvider", "MessageProvider", "StepProvider");

		var dataTypeImpls = contributors.values().stream().flatMap(m -> m.getOrDefault("DataTypeProvider", List.of()).stream()).toList();
		var messageImpls  = contributors.values().stream().flatMap(m -> m.getOrDefault("MessageProvider",  List.of()).stream()).toList();
		var stepImpls     = contributors.values().stream().flatMap(m -> m.getOrDefault("StepProvider",     List.of()).stream()).toList();

		assertThat(dataTypeImpls).anyMatch(s -> s.endsWith("CoreDataTypes"));
		assertThat(messageImpls)
			.anyMatch(s -> s.endsWith("AssertionMessageProvider"))
			.anyMatch(s -> s.endsWith("CoreStepMessageProvider"));
		assertThat(stepImpls).anyMatch(s -> s.endsWith("CoreStepProvider"));
	}

	@Test
	void getRepositoryFailsWithoutRepositoryFactory() {
		var runtime = new OpenBBTRuntime(TEST_CONFIG);

		assertThatThrownBy(() -> runtime.getRepository(TestPlanRepository.class))
			.isInstanceOf(OpenBBTException.class)
			.hasMessageContaining("No PlanRepositoryFactory found");
	}
}
