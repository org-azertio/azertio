package org.azertio.plugins.messaging;

import org.myjtools.imconfig.Config;
import org.myjtools.jexten.Extension;
import org.myjtools.jexten.Inject;
import org.myjtools.jexten.Scope;
import org.azertio.core.ContentTypes;
import org.azertio.core.backend.ExecutionContext;
import org.azertio.core.contributors.ContentType;
import org.azertio.core.contributors.StepExpression;
import org.azertio.core.contributors.StepProvider;
import org.azertio.core.contributors.TearDown;
import org.azertio.core.testplan.Document;

import java.util.HashMap;
import java.util.Map;

@Extension(
    name = "Messaging steps provider",
    scope = Scope.TRANSIENT,
    extensionPointVersion = "1.0"
)
public class MessagingStepProvider implements StepProvider {

    @Inject
    ContentTypes contentTypes;

    private final Map<String, JmsMessagingEngine> engines = new HashMap<>();
    private String currentAlias;
    private int defaultTimeout;

    @Override
    public void init(Config config) {
        defaultTimeout = config.getInteger("messaging.timeout").orElse(10);
        Config systems = config.inner("messaging.systems");
        for (String alias : systems.innerKeys().toList()) {
            Config sys = systems.inner(alias);
            String factoryClass = sys.getString("connectionFactoryClass").orElseThrow(() ->
                new IllegalStateException("messaging.systems." + alias + ".connectionFactoryClass is required")
            );
            String brokerUrl = sys.getString("brokerUrl").orElse("tcp://localhost:61616");
            String username  = sys.getString("username").orElse("");
            String password  = sys.getString("password").orElse("");
            JmsMessagingEngine engine = new JmsMessagingEngine();
            try {
                engine.init(factoryClass, brokerUrl, username, password);
            } catch (Exception e) {
                throw new IllegalStateException(
                    "Failed to connect to JMS system '" + alias + "'. " +
                    "Ensure the provider JAR is declared with `with` in azertio.yaml. Cause: " + e.getMessage(), e
                );
            }
            engines.put(alias, engine);
        }
    }

    @TearDown
    public void close() {
        engines.values().forEach(engine -> {
            try { engine.close(); } catch (Exception ignored) { }
        });
    }

    @StepExpression(value = "messaging.use", args = {"alias:text"})
    public void use(String alias) {
        if (!engines.containsKey(alias)) {
            throw new IllegalArgumentException(
                "No messaging system configured with alias '" + alias + "'. " +
                "Available: " + engines.keySet()
            );
        }
        this.currentAlias = alias;
    }

    @StepExpression(value = "messaging.subscribe", args = {"destination:text"})
    public void subscribe(String destination) {
        activeEngine().subscribe(interpolate(destination));
    }

    @StepExpression(value = "messaging.publish", args = {"destination:text"})
    public void publish(String destination, Document body) {
        activeEngine().publish(interpolate(destination), interpolate(body.content()));
    }

    @StepExpression(value = "messaging.publish.keyed", args = {"destination:text", "key:text"})
    public void publishKeyed(String destination, String key, Document body) {
        activeEngine().publish(interpolate(destination), interpolate(key), interpolate(body.content()));
    }

    @StepExpression(value = "messaging.assert.received", args = {"destination:text"})
    public void assertReceived(String destination, Document expected) {
        String actual = activeEngine().pollNext(interpolate(destination), defaultTimeout);
        String expectedContent = interpolate(expected.content());
        String mimeType = expected.mimeType() != null ? expected.mimeType() : "text/plain";
        contentTypes.get(mimeType).ifPresentOrElse(
            ct -> ct.assertContentEquals(expectedContent, actual, ContentType.ComparisonMode.LOOSE),
            () -> {
                if (!actual.contains(expectedContent)) {
                    throw new AssertionError(
                        "Message on '" + destination + "' did not contain expected content.\n" +
                        "Expected (contains): " + expectedContent + "\nActual: " + actual
                    );
                }
            }
        );
    }

    @StepExpression(value = "messaging.assert.received.exact", args = {"destination:text"})
    public void assertReceivedExact(String destination, Document expected) {
        String actual = activeEngine().pollNext(interpolate(destination), defaultTimeout);
        String expectedContent = interpolate(expected.content());
        String mimeType = expected.mimeType() != null ? expected.mimeType() : "text/plain";
        contentTypes.get(mimeType).ifPresentOrElse(
            ct -> ct.assertContentEquals(expectedContent, actual, ContentType.ComparisonMode.STRICT),
            () -> {
                if (!actual.equals(expectedContent)) {
                    throw new AssertionError(
                        "Message on '" + destination + "' did not match exactly.\n" +
                        "Expected: " + expectedContent + "\nActual: " + actual
                    );
                }
            }
        );
    }

    @StepExpression(value = "messaging.extract.field", args = {"destination:text", "field:text", "variable:id"})
    public void extractField(String destination, String field, String variable) {
        String actual = activeEngine().pollNext(interpolate(destination), defaultTimeout);
        String value = contentTypes.get("application/json").orElseThrow(() ->
            new IllegalStateException("No JSON ContentType handler available")
        ).extractValue(actual, interpolate(field));
        ExecutionContext.current().setVariable(variable, value);
    }

    private JmsMessagingEngine activeEngine() {
        if (currentAlias == null) {
            if (engines.size() == 1) {
                return engines.values().iterator().next();
            }
            throw new IllegalStateException(
                "Multiple messaging systems configured. Use messaging.use <alias> to select one."
            );
        }
        return engines.get(currentAlias);
    }

    protected String interpolate(String text) {
        return ExecutionContext.current().interpolateString(text);
    }

}