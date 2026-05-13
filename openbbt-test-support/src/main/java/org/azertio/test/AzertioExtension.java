package org.azertio.test;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

/**
 * JUnit 5 extension that resolves {@link JUnitAzertioPlan} parameters for integration tests.
 * <p>
 * The test method must be annotated with {@link FeatureDir} to indicate which subdirectory
 * under {@code /features/} in the test classpath contains the feature files to execute.
 *
 * <pre>{@code
 * @ExtendWith(AzertioExtension.class)
 * class MyPluginTest {
 *
 *     @Test
 *     @FeatureDir("scenario-a")
 *     void scenario_passes(JUnitAzertioPlan plan) {
 *         plan.withConfig("some.key", "value")
 *             .execute()
 *             .assertAllPassed();
 *     }
 * }
 * }</pre>
 */
public class AzertioExtension implements ParameterResolver, AfterEachCallback {

    private static final Namespace NAMESPACE = Namespace.create(AzertioExtension.class);
    private static final String TEMP_DIR_KEY = "tempDir";

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return parameterContext.getParameter().getType().equals(JUnitAzertioPlan.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        try {
            Method testMethod = extensionContext.getRequiredTestMethod();
            FeatureDir annotation = testMethod.getAnnotation(FeatureDir.class);
            if (annotation == null) {
                throw new ParameterResolutionException(
                    "Test method '" + testMethod.getName() + "' must be annotated with @FeatureDir");
            }

            String featureDir = annotation.value();
            URL resource = extensionContext.getRequiredTestClass().getResource("/features/" + featureDir);
            if (resource == null) {
                throw new ParameterResolutionException(
                    "Feature directory not found in test classpath: /features/" + featureDir);
            }
            Path featureDirPath = Path.of(resource.toURI());
            Path tempDir = Files.createTempDirectory("azertio-test-");

            extensionContext.getStore(NAMESPACE).put(TEMP_DIR_KEY, tempDir);

            return new JUnitAzertioPlan(featureDirPath, tempDir);

        } catch (ParameterResolutionException e) {
            throw e;
        } catch (Exception e) {
            throw new ParameterResolutionException("Failed to resolve JUnitAzertioPlan parameter", e);
        }
    }

    @Override
    public void afterEach(ExtensionContext extensionContext) {
        Store store = extensionContext.getStore(NAMESPACE);
        Path tempDir = store.remove(TEMP_DIR_KEY, Path.class);
        if (tempDir != null) {
            deleteQuietly(tempDir);
        }
    }

    private void deleteQuietly(Path dir) {
        try {
            if (Files.exists(dir)) {
                Files.walk(dir)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try { Files.delete(path); } catch (IOException ignored) {}
                    });
            }
        } catch (IOException ignored) {}
    }
}
