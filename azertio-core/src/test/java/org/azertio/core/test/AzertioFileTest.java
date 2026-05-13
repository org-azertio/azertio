package org.azertio.core.test;

import org.junit.jupiter.api.Test;
import org.myjtools.imconfig.Config;
import org.azertio.core.AzertioConfig;
import org.azertio.core.AzertioFile;
import org.azertio.core.testplan.TagExpression;
import org.azertio.core.testplan.TestProject;
import org.azertio.core.testplan.TestSuite;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

class AzertioFileTest {

	private final Config INPUT_ENV = Config.ofMap(Map.of(AzertioConfig.ENV_PATH, "target/.azertio"));

	@Test
	void testReadFile() throws IOException {
		try (var reader = Files.newBufferedReader(Path.of("src/test/resources/azertio.yaml"), StandardCharsets.UTF_8)) {
			var file = AzertioFile.read(reader);
			assertThat(file).isNotNull();
			assertThat(file.project()).extracting(TestProject::name).isEqualTo("My Project");
			assertThat(file.project().testSuites()).containsExactlyInAnyOrder(
				new TestSuite("suiteA", "Suite A", TagExpression.parse("A or Aa")),
				new TestSuite("suiteB", "Suite B", TagExpression.parse("B or Bb"))
			);
			assertThat(file.plugins()).containsExactlyInAnyOrder(
				"gherkin",
				"org.azertio.plugins:gherkin-azertio-plugin",
				"org.azertio.plugins:gherkin-azertio-plugin:1.0.0"
			);
		}
	}

	@Test
	void testCreateContext() throws IOException {
		try (var reader = Files.newBufferedReader(Path.of("src/test/resources/azertio.yaml"), StandardCharsets.UTF_8)) {
			var file = AzertioFile.read(reader);
			var context = file.createContext(INPUT_ENV, List.of("suiteA"));
			assertThat(context).isNotNull();
			assertThat(context.testProject()).extracting(TestProject::name).isEqualTo("My Project");
			assertThat(context.testSuites()).containsExactlyInAnyOrder(
				"suiteA"
			);
			System.out.println(context.configuration().toString());
			// Properties from the file should be present, and not resolved (i.e. still contain
			// the ENV_VAR_ONE placeholder)
			// Also, properties from the file should override any provided via the environment
			// (i.e. ENV_VAR_ONE should not resolve to "A")
			// This is because resolution of properties should be a separate step, and we want to ensure that the
			// file properties are correctly loaded and take precedence over environment variables
			assertThat(context.configuration().getString("gherkin.gherkin-prop")).hasValue("{{ENV_VAR_ONE}}");
			assertThat(context.configuration().getString("gherkin.gherkin-param")).hasValue("{{param1}}");
			assertThat(context.configuration().getString("rest.rest-prop")).hasValue("{{propertyOne}}");
			assertThat(context.plugins()).containsExactlyInAnyOrder(
				"org.azertio.plugins:gherkin-azertio-plugin",
				"org.azertio.plugins:gherkin-azertio-plugin",
				"org.azertio.plugins:gherkin-azertio-plugin:1.0.0"
			);
		}
	}

}
