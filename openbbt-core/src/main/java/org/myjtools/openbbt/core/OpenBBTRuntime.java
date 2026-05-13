package org.myjtools.openbbt.core;

import org.myjtools.imconfig.Config;
import org.myjtools.jexten.ExtensionManager;
import org.myjtools.jexten.InjectionProvider;
import org.myjtools.jexten.ModuleLayerProvider;
import org.myjtools.openbbt.core.contributors.*;
import org.myjtools.openbbt.core.events.EventBus;
import org.myjtools.openbbt.core.execution.Profile;
import org.myjtools.openbbt.core.messages.MessageProvider;
import org.myjtools.openbbt.core.messages.Messages;
import org.myjtools.openbbt.core.persistence.AttachmentRepository;
import org.myjtools.openbbt.core.persistence.Repository;
import org.myjtools.openbbt.core.persistence.TestExecutionRepository;
import org.myjtools.openbbt.core.persistence.TestPlanRepository;
import org.myjtools.openbbt.core.testplan.PlanBuilder;
import org.myjtools.openbbt.core.testplan.TestPlan;
import org.myjtools.openbbt.core.util.Lazy;
import org.myjtools.openbbt.core.util.Log;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

public class OpenBBTRuntime implements InjectionProvider {


	private static final Log log = Log.of();

	private final Clock clock;
	private final ExtensionManager extensionManager;
	private final OpenBBTPluginManager pluginManager;
	private final Config config;
	private final PlanBuilder planBuilder;
	private final ResourceFinder resourceFinder;
	private final ResourceSet resourceSet;
	private final ContentTypes contentTypes;
	private final RepositoryFactory repositoryFactory;
	private final boolean readOnly;
	private final Lazy<TestPlanRepository> planNodeRepository = Lazy.of(this::openRepository);
	private final Lazy<TestExecutionRepository> executionRepository = Lazy.of(this::openExecutionRepository);
	private final Lazy<AttachmentRepository> attachmentRepository = Lazy.of(this::openAttachmentRepository);
	private final Lazy<DataTypes> dataTypes = Lazy.of(this::collectDataTypes);
	private final Profile profile;
	private final EventBus eventBus;

	public OpenBBTRuntime(Config configuration) {
		this(configuration, Instant::now);
	}


	public OpenBBTRuntime(Config configuration, Clock clock) {
		this.profile = Profile.NONE;
		this.readOnly = false;
		this.clock = clock;
		this.pluginManager = new OpenBBTPluginManager(configuration);
		this.extensionManager = ExtensionManager
			.create(ModuleLayerProvider.compose(ModuleLayerProvider.boot(),pluginManager.moduleLayerProvider()))
			.withInjectionProvider(this);
		Config rawConfig = extensionManager.getExtensions(ConfigProvider.class)
			.map(ConfigProvider::config)
			.reduce(Config.empty(), Config::append)
			.append(configuration);
		this.config = profile.applyProfile(rawConfig);
		this.repositoryFactory = extensionManager.getExtension(RepositoryFactory.class)
			.orElse(null);
		this.resourceFinder = new ResourceFinder(config.get(OpenBBTConfig.RESOURCE_PATH, Path::of).orElseThrow(
			()-> new OpenBBTException("Resource path not configured {}: ",OpenBBTConfig.RESOURCE_PATH)
		));
		this.resourceSet = resourceFinder.findResources(configuration().getString(OpenBBTConfig.RESOURCE_FILTER).orElseThrow(
			()-> new OpenBBTException("Resource filter not configured {}: ",OpenBBTConfig.RESOURCE_FILTER)
		));
		if (this.resourceSet.isEmpty()) {
			log.warn("No resources found with path {} and filter {}",
			configuration().getString(OpenBBTConfig.RESOURCE_PATH).orElse(""),
			configuration().getString(OpenBBTConfig.RESOURCE_FILTER).orElse(""));
		}
		this.contentTypes = ContentTypes.of(extensionManager.getExtensions(ContentType.class).toList());
		this.planBuilder = new PlanBuilder(this);
		this.eventBus = new EventBus();
		getExtensions(EventObserver.class).forEach(eventBus::registerObserver);
	}


	private OpenBBTRuntime(OpenBBTRuntime copy, Profile profile) {
		this.clock = copy.clock;
		this.extensionManager = copy.extensionManager;
		this.pluginManager = copy.pluginManager;
		this.config = profile.applyProfile(copy.config);
		this.repositoryFactory = copy.repositoryFactory;
		this.resourceFinder = copy.resourceFinder;
		this.resourceSet = copy.resourceSet;
		this.planBuilder = copy.planBuilder;
		this.contentTypes = copy.contentTypes;
		this.readOnly = copy.readOnly;
		this.profile = profile;
		this.eventBus = copy.eventBus;
	}


	public OpenBBTRuntime withProfile(Profile profile) {
		return new OpenBBTRuntime(this,profile);
	}


	/**
	 * Creates a lightweight runtime that only initializes the repository, skipping plugin
	 * loading and resource scanning. Use this when only read access to stored plans is needed.
	 */
	public static OpenBBTRuntime repositoryOnly(Config configuration) {
		return new OpenBBTRuntime(configuration, false);
	}


	/*
	 * Private constructor for repository-only runtime. The 'ignored' parameter is just a dummy to differentiate the signature.
	 */
	private OpenBBTRuntime(Config configuration, boolean ignored) {
		this.profile = Profile.NONE;
		this.readOnly = true;
		this.clock = Instant::now;
		this.pluginManager = null;
		this.extensionManager = ExtensionManager
			.create(ModuleLayerProvider.boot())
			.withInjectionProvider(this);
		var rawConfig = extensionManager.getExtensions(ConfigProvider.class)
			.map(ConfigProvider::config)
			.reduce(Config.empty(), Config::append)
			.append(configuration);
		this.config = profile.applyProfile(rawConfig);
		this.repositoryFactory = extensionManager.getExtension(RepositoryFactory.class)
			.orElse(null);
		this.resourceFinder = null;
		this.resourceSet = null;
		this.planBuilder = null;
		this.contentTypes = null;
		this.eventBus = new EventBus();
		getExtensions(EventObserver.class).forEach(eventBus::registerObserver);
	}


	public Config configuration() {
		return config;
	}

	public Clock clock() {
		return clock;
	}




	@Override
	public Stream<Object> provideInstancesFor(Class<?> type, String name) {

		if (type == Config.class) {
			if (name == null || name.isEmpty()) {
				return Stream.of(config);
			} else {
				return Stream.of(config.inner(name));
			}
		}

		if (repositoryFactory == null &&
				(type == TestPlanRepository.class ||
				type == TestExecutionRepository.class ||
				type == AttachmentRepository.class)) {
			return Stream.empty();
		}

		if (type == TestPlanRepository.class) {
			return Stream.of(planNodeRepository.get());
		}

		if (type == TestExecutionRepository.class) {
			return Stream.of(executionRepository.get());
		}
		if (type == AttachmentRepository.class) {
			return Stream.of(attachmentRepository.get());
		}
		if (type == DataTypes.class) {
			return Stream.of(dataTypes.get());
		}
		if (type == Messages.class) {
			return Stream.of(Messages.of(
					getExtensions(MessageProvider.class).filter(it -> it.providerFor(name)).toList()
			));
		}
		if (type == ResourceFinder.class) {
			return streamOf(resourceFinder);
		}
		if (type == ResourceSet.class) {
			return streamOf(resourceSet);
		}
		if (type == Clock.class) {
			return streamOf(clock);
		}
		if (type == ContentTypes.class) {
			return streamOf(contentTypes);
		}
		if (type == EventBus.class) {
			return streamOf(eventBus);
		}
		return Stream.empty();
	}

	private <T> Stream<T> streamOf(T instance) {
		return instance != null ? Stream.of(instance) : Stream.empty();
	}


	public <T> Stream<T> getExtensions(Class<T> type) {
		return extensionManager.getExtensions(type);
	}


	public List<Class<?>> getContributedTypes() {
		return List.of(
			ConfigProvider.class,
			MessageProvider.class,
			RepositoryFactory.class,
			ContentType.class,
			DataTypeProvider.class,
			ReportBuilder.class,
			StepProvider.class,
			SuiteAssembler.class,
			AIIndexProvider.class
		);
	}

	public String getStepIndex() {
		var parts = getExtensions(AIIndexProvider.class)
			.map(AIIndexProvider::stepIndexJson)
			.filter(s -> s != null && !s.isBlank())
			.map(String::strip)
			.filter(s -> s.startsWith("[") && s.endsWith("]"))
			.map(s -> s.substring(1, s.length() - 1).strip())
			.filter(s -> !s.isEmpty())
			.toList();
		if (parts.isEmpty()) return "[]";
		return "[\n" + String.join(",\n", parts) + "\n]";
	}


	public Map<String, Map<String,List<String>>> getContributors() {
		Map<String, Map<String,List<String>>> result = new TreeMap<>();
		for (Class<?> type : getContributedTypes()) {
			try {
				long count = getExtensions(type).peek(ext -> result
					.computeIfAbsent(ext.getClass().getModule().getName(), k -> new TreeMap<>())
					.computeIfAbsent(type.getSimpleName(), k -> new ArrayList<>())
					.add(ext.getClass().getSimpleName())
				).count();
				log.info("Contributors of type {}: {}", type.getSimpleName(), count);
			} catch (Exception e) {
				log.error(e, "Failed to load contributors of type {}", type.getSimpleName());
			}
		}
		result.values().forEach(types -> types.values().forEach(Collections::sort));
		return result;
	}


	public <T extends Repository> T getRepository(Class<?> type) {
		if (repositoryFactory == null) {
			throw new OpenBBTException("No PlanRepositoryFactory found, cannot create PlanRepository");
		}
		if (type == TestPlanRepository.class) {
			return (T) planNodeRepository.get();
		} else if (type == TestExecutionRepository.class) {
			return (T) executionRepository.get();
		} else if (type == AttachmentRepository.class) {
			return (T) attachmentRepository.get();
		} else {
			throw new OpenBBTException("Unsupported repository type requested: {}", type.getSimpleName());
		}
	}


	private TestPlanRepository openRepository() {
		if (readOnly) {
			return repositoryFactory.createReadOnlyRepository(TestPlanRepository.class);
		}
		return createRepository(TestPlanRepository.class);
	}

	private TestExecutionRepository openExecutionRepository() {
		if (readOnly) {
			return repositoryFactory.createReadOnlyRepository(TestExecutionRepository.class);
		}
		return createRepository(TestExecutionRepository.class);
	}

	private AttachmentRepository openAttachmentRepository() {
		return createRepository(AttachmentRepository.class);
	}

	private <T extends Repository> T createRepository(Class<T> type) {
		if (repositoryFactory == null) {
			log.warn("No RepositoryFactory found, cannot create {}", type.getSimpleName());
			return null;
		}
		return repositoryFactory.createRepository(type);
	}


	private DataTypes collectDataTypes() {
		var dataTypeList = getExtensions(DataTypeProvider.class).flatMap(DataTypeProvider::dataTypes).toList();
		return DataTypes.of(dataTypeList);
	}

	public ResourceSet resourceSet() {
		return resourceSet;
	}


	public TestPlan buildTestPlan(OpenBBTContext context) {
		return planBuilder.buildTestPlan(context);
	}

	public TestPlan buildTestPlan(OpenBBTContext context, List<String> selectedSuites) {
		return planBuilder.buildTestPlan(context, selectedSuites);
	}


	public Profile profile() {
		return profile;
	}

	public EventBus eventBus() {
		return eventBus;
	}

}
