package org.azertio.core;

import org.myjtools.imconfig.Config;
import org.azertio.core.testplan.TagExpression;
import org.azertio.core.testplan.TestProject;
import org.azertio.core.testplan.TestSuite;
import org.yaml.snakeyaml.Yaml;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AzertioFile {

	public static AzertioFile read(Reader reader) {
		Yaml yaml = new Yaml();
		@SuppressWarnings("unchecked")
		Map<String, Object> raw = yaml.load(reader);

		AzertioFile file = new AzertioFile();

		@SuppressWarnings("unchecked")
		Map<String, Object> projectMap = (Map<String, Object>) raw.get("project");
		if (projectMap != null) {
			file.testProject = parseProject(projectMap);
		}
		@SuppressWarnings("unchecked")
		List<String> plugins = (List<String>) raw.get("plugins");
		file.plugins = plugins;
		@SuppressWarnings("unchecked")
		Map<String, Object> configuration = (Map<String, Object>) raw.get("configuration");
		file.configuration = configuration;
		@SuppressWarnings("unchecked")
		Map<String, Map<String,String>> profiles = (Map<String, Map<String,String>>) raw.get("profiles");
		file.profiles = profiles;

		return file;
	}

	private static TestProject parseProject(Map<String, Object> map) {
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> suitesRaw = (List<Map<String, Object>>) map.getOrDefault("test-suites", List.of());
		List<TestSuite> testSuites = suitesRaw.stream().map(AzertioFile::parseTestSuite).toList();
		return new TestProject((String) map.get("name"), (String) map.get("description"), (String) map.get("organization"), testSuites);
	}

	private static TestSuite parseTestSuite(Map<String, Object> map) {
		TagExpression tagExpression = TagExpression.parse((String) map.get("tag-expression"));
		return new TestSuite((String) map.get("name"), (String) map.get("description"), tagExpression);
	}

	private TestProject testProject;
	private List<String> plugins;
	private Map<String, Object> configuration;
	private Map<String, Map<String,String>> profiles;


	public TestProject project() {
		return testProject;
	}

	public List<String> plugins() {
		return List.copyOf(plugins);
	}

	public Map<String, Object> configuration() {
		return configuration != null ? Map.copyOf(configuration) : Map.of();
	}

	public Map<String, Map<String,String>> profiles() {
		return profiles != null ? Map.copyOf(profiles) : Map.of();
	}


	public AzertioContext createContext(Config inputParameters,	List<String> testSuites) {
		var contextTestSuites = new ArrayList<String>();
		TestProject contextProject;
		if (testSuites.isEmpty()) {
			if (testProject.testSuites().isEmpty()) {
				var defaultSuite = new TestSuite("default", null, TagExpression.EMPTY);
				contextProject = new TestProject(
					testProject.name(),
					testProject.description(),
					testProject.organization(),
					List.of(defaultSuite)
				);
				contextTestSuites.add("default");
			} else {
				contextProject = new TestProject(
					testProject.name(),
					testProject.description(),
					testProject.organization(),
					List.copyOf(testProject.testSuites())
				);
				testProject.testSuites().forEach(suite -> contextTestSuites.add(suite.name()));
			}
		} else {
			contextProject = new TestProject(
				testProject.name(),
				testProject.description(),
				testProject.organization(),
				List.copyOf(testProject.testSuites())
			);
			for (String testSuiteName : testSuites) {
				var testSuite = testProject.testSuites().stream().filter(suite -> suite.name().equals(testSuiteName)).findFirst();
				if (testSuite.isEmpty()) {
					throw new AzertioException("Test suite '" + testSuiteName + "' not found in project file");
				}
				contextTestSuites.add(testSuite.map(TestSuite::name).orElseThrow());
			}
		}
		Map<String,Object> contextConfiguration = this.configuration != null ? this.configuration : new HashMap<>();
		List<String> contextPlugins = List.of();
		if (this.plugins != null) {
			contextPlugins = this.plugins.stream().map(this::normalizePluginName).toList();
		}
		return new AzertioContext(
			contextProject,
			Config.ofMap(flattenMap(contextConfiguration, "")).append(inputParameters),
			contextTestSuites,
			contextPlugins
		);
	}


	private Map<String, Object> flattenMap(Map<String, Object> map, String prefix) {
		Map<String, Object> result = new HashMap<>();
		for (Map.Entry<String, Object> entry : map.entrySet()) {
			String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
			if (entry.getValue() instanceof Map<?, ?> nested) {
				result.putAll(flattenMap((Map<String, Object>) nested, key));
			} else {
				result.put(key, entry.getValue());
			}
		}
		return result;
	}


	private String normalizePluginName(String name) {
		String suffix = "";
		String pluginPart = name;
		int withIndex = name.indexOf(" with ");
		if (withIndex != -1) {
			pluginPart = name.substring(0, withIndex);
			suffix = name.substring(withIndex);
		}
		if (!pluginPart.contains(":")) {
			pluginPart = "org.azertio.plugins:" + pluginPart + "-azertio-plugin";
		}
		return pluginPart + suffix;
	}





}


