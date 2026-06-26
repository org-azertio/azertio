Create a new Azertio plugin scaffold from scratch.

**Usage:** `/new-plugin <name> "<Display Name>" "<description>"`

- `<name>`: lowercase identifier, e.g. `email` → generates `email-azertio-plugin`, `EmailStepProvider`, package `org.azertio.plugins.email`
- `<Display Name>`: human-readable name, e.g. `"Email"`
- `<description>`: short Maven description, e.g. `"A plugin that provides email-sending steps"`

If arguments are missing, ask the user for: name, display name, and description.

---

Given `$ARGUMENTS`, parse: `name` (first word), `displayName` (second quoted string), `description` (third quoted string).

Derive the following values:
- `pluginDir` = `plugins/<name>-azertio-plugin`
- `artifactId` = `<name>-azertio-plugin`
- `javaPackage` = `org.azertio.plugins.<name>`
- `moduleMain` = `org.azertio.plugins.<name>`
- `moduleTest` = `org.azertio.plugins.<name>.test`
- `ClassName` = PascalCase of `<name>` (e.g. `email` → `Email`, `web-ui` → `WebUi`)
- `keyPrefix` = `<name>` (e.g. `webui.`, `email.`)

Create ALL of the following files. Do not skip any.

---

### `<pluginDir>/pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.azertio</groupId>
        <artifactId>azertio-plugin-starter</artifactId>
        <relativePath/>
        <version>1.0.0</version>
    </parent>

    <groupId>org.azertio.plugins</groupId>
    <artifactId>{artifactId}</artifactId>
    <version>1.0.0-SNAPSHOT</version>

    <name>{displayName} Azertio Plugin</name>
    <description>{description}</description>

    <properties>
        <azertio-core.version>1.0.0</azertio-core.version>
        <azertio.docgen.stepDocFile>${project.basedir}/src/main/resources/steps.yaml</azertio.docgen.stepDocFile>
        <azertio.docgen.configFile>${project.basedir}/src/main/resources/config.yaml</azertio.docgen.configFile>
        <azertio.docgen.stepTitle>{displayName} Plugin — Step Reference</azertio.docgen.stepTitle>
        <azertio.docgen.configTitle>{displayName} Plugin — Configuration Reference</azertio.docgen.configTitle>
    </properties>

    <licenses>
        <license>
            <name>MIT License</name>
            <url>https://opensource.org/licenses/MIT</url>
            <distribution>repo</distribution>
        </license>
    </licenses>

    <url>https://azertio.org/plugins/{name}</url>

    <dependencies>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter-api</artifactId>
            <version>5.9.3</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter-engine</artifactId>
            <version>5.9.3</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.assertj</groupId>
            <artifactId>assertj-core</artifactId>
            <version>3.27.7</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.azertio</groupId>
            <artifactId>azertio-persistence</artifactId>
            <version>${azertio-core.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.azertio.plugins</groupId>
            <artifactId>gherkin-azertio-plugin</artifactId>
            <version>${azertio-core.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.azertio</groupId>
            <artifactId>azertio-test-support</artifactId>
            <version>${azertio-core.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>ch.qos.logback</groupId>
            <artifactId>logback-classic</artifactId>
            <version>1.5.25</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <executions>
                    <execution>
                        <id>default-testCompile</id>
                        <goals><goal>testCompile</goal></goals>
                        <configuration combine.children="append">
                            <compilerArgs>
                                <arg>--add-reads</arg>
                                <arg>{moduleTest}=ALL-UNNAMED</arg>
                            </compilerArgs>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <configuration>
                    <classpathDependencyExcludes>
                        <classpathDependencyExclude>org.slf4j:slf4j-simple</classpathDependencyExclude>
                    </classpathDependencyExcludes>
                    <argLine>@{argLine} --add-reads {moduleTest}=ALL-UNNAMED</argLine>
                </configuration>
            </plugin>
        </plugins>
    </build>

</project>
```

---

### `<pluginDir>/src/main/java/module-info.java`

```java
import {javaPackage}.{ClassName}AIIndexProvider;
import {javaPackage}.{ClassName}ConfigHelpProvider;
import {javaPackage}.{ClassName}ConfigProvider;
import {javaPackage}.{ClassName}MessageProvider;
import {javaPackage}.{ClassName}StepHelpProvider;
import {javaPackage}.{ClassName}StepProvider;

module {moduleMain} {

    requires org.myjtools.jexten;
    requires org.azertio.core;
    requires org.myjtools.imconfig;

    provides org.azertio.core.contributors.StepProvider with {ClassName}StepProvider;
    provides org.azertio.core.contributors.ConfigProvider with {ClassName}ConfigProvider;
    provides org.azertio.core.messages.MessageProvider with {ClassName}MessageProvider;
    provides org.azertio.core.contributors.AIIndexProvider with {ClassName}AIIndexProvider;
    provides org.azertio.core.contributors.HelpProvider with {ClassName}StepHelpProvider, {ClassName}ConfigHelpProvider;

    exports {javaPackage};
    opens {javaPackage} to org.myjtools.jexten;

}
```

---

### `<pluginDir>/src/main/java/<javaPackage path>/{ClassName}StepProvider.java`

```java
package {javaPackage};

import org.myjtools.imconfig.Config;
import org.myjtools.jexten.Extension;
import org.myjtools.jexten.Scope;
import org.azertio.core.backend.ExecutionContext;
import org.azertio.core.contributors.StepProvider;

@Extension(
    name = "{displayName} steps provider",
    scope = Scope.TRANSIENT,
    extensionPointVersion = "1.0"
)
public class {ClassName}StepProvider implements StepProvider {

    @Override
    public void init(Config config) {
    }

    protected String interpolate(String text) {
        return ExecutionContext.current().interpolateString(text);
    }

}
```

---

### `<pluginDir>/src/main/java/<javaPackage path>/{ClassName}ConfigProvider.java`

```java
package {javaPackage};

import org.myjtools.jexten.Extension;
import org.azertio.core.ConfigAdapter;
import org.azertio.core.contributors.ConfigProvider;

@Extension
public class {ClassName}ConfigProvider extends ConfigAdapter implements ConfigProvider {

    @Override
    protected String resource() {
        return "config.yaml";
    }

}
```

---

### `<pluginDir>/src/main/java/<javaPackage path>/{ClassName}MessageProvider.java`

```java
package {javaPackage};

import org.myjtools.jexten.Extension;
import org.azertio.core.messages.MessageProvider;
import org.azertio.core.messages.StepDocMessageAdapter;

import java.util.LinkedHashMap;
import java.util.Map;

@Extension
public class {ClassName}MessageProvider extends StepDocMessageAdapter implements MessageProvider {

    public {ClassName}MessageProvider() {
        super("steps.yaml");
    }

    @Override
    protected Map<String, String> languageResources() {
        var map = new LinkedHashMap<String, String>();
        map.put("dsl", "steps_dsl.yaml");
        map.put("en",  "steps_en.yaml");
        map.put("es",  "steps_es.yaml");
        return map;
    }

    @Override
    public boolean providerFor(String category) {
        return {ClassName}StepProvider.class.getSimpleName().equals(category);
    }

}
```

---

### `<pluginDir>/src/main/java/<javaPackage path>/{ClassName}AIIndexProvider.java`

```java
package {javaPackage};

import org.myjtools.jexten.Extension;
import org.azertio.core.contributors.AIIndexProvider;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Extension
public class {ClassName}AIIndexProvider implements AIIndexProvider {

    @Override
    public String stepIndexJson() {
        try (var stream = getClass().getModule().getResourceAsStream("steps-index.json")) {
            if (stream == null) { return "[]"; }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "[]";
        }
    }

}
```

---

### `<pluginDir>/src/main/java/<javaPackage path>/{ClassName}StepHelpProvider.java`

```java
package {javaPackage};

import org.myjtools.jexten.Extension;
import org.myjtools.jexten.Scope;
import org.azertio.core.contributors.HelpProvider;
import org.azertio.core.help.StepHelpAdapter;

import java.util.Map;

@Extension(scope = Scope.SINGLETON)
public class {ClassName}StepHelpProvider extends StepHelpAdapter implements HelpProvider {

    public {ClassName}StepHelpProvider() {
        super("{keyPrefix}.steps", "{displayName} Steps", "{displayName} Steps", "steps.yaml", Map.of(
            "dsl", "steps_dsl.yaml",
            "en",  "steps_en.yaml",
            "es",  "steps_es.yaml"
        ));
    }

}
```

---

### `<pluginDir>/src/main/java/<javaPackage path>/{ClassName}ConfigHelpProvider.java`

```java
package {javaPackage};

import org.myjtools.jexten.Extension;
import org.myjtools.jexten.Scope;
import org.azertio.core.contributors.HelpProvider;
import org.azertio.core.help.ConfigHelpAdapter;

@Extension(scope = Scope.SINGLETON)
public class {ClassName}ConfigHelpProvider extends ConfigHelpAdapter implements HelpProvider {

    public {ClassName}ConfigHelpProvider() {
        super("{keyPrefix}.config", "{displayName} Configuration", "{displayName} Configuration", "config.yaml");
    }

}
```

---

### `<pluginDir>/src/main/resources/config.yaml`

```yaml
{keyPrefix}.example-key:
  description: |
    Example configuration key — replace with actual config keys for this plugin.
  type: text
  defaultValue: example-value
```

---

### `<pluginDir>/src/main/resources/steps.yaml`

```yaml
# Step definitions for the {displayName} plugin.
# Each entry documents one step key used by @StepExpression in {ClassName}StepProvider.
```

---

### `<pluginDir>/src/main/resources/steps_en.yaml`

```yaml
# English step expressions for the {displayName} plugin.
```

---

### `<pluginDir>/src/main/resources/steps_es.yaml`

```yaml
# Expresiones de pasos en español para el plugin {displayName}.
```

---

### `<pluginDir>/src/main/resources/steps_dsl.yaml`

```yaml
# DSL step expressions for the {displayName} plugin.
```

---

### `<pluginDir>/src/test/java/module-info.java`

```java
module {moduleTest} {

    requires {moduleMain};

    requires org.junit.jupiter.api;
    requires org.azertio.persistence;
    requires org.azertio.plugins.gherkin;
    requires org.myjtools.imconfig;
    requires org.azertio.core;
    requires azertio.test.support;

    opens {javaPackage}.test to org.junit.platform.commons;

}
```

---

### `<pluginDir>/src/test/java/<javaPackage path>/test/Test{ClassName}StepProvider.java`

```java
package {javaPackage}.test;

import org.junit.jupiter.api.Test;
import org.myjtools.imconfig.Config;
import org.azertio.core.AzertioRuntime;

import java.util.Map;

class Test{ClassName}StepProvider {

    @Test
    void pluginLoadsConfiguration() {
        Config config = Config.ofMap(Map.of(
            "core.resourcePath", "src/test/resources"
        ));
        AzertioRuntime runtime = new AzertioRuntime(config);
        assert runtime != null;
    }

}
```

---

### `<pluginDir>/src/test/resources/logback-test.xml`

```xml
<configuration>

    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss} %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <logger name="org.azertio" level="DEBUG"/>

    <root level="OFF">
        <appender-ref ref="CONSOLE"/>
    </root>

</configuration>
```

---

After creating all files, print a summary listing every file created and remind the user to:
1. Add any plugin-specific `requires` in `module-info.java` (main)
2. Add Maven dependencies in `pom.xml` for the plugin's own libraries
3. Replace the placeholder `config.yaml` entries with real config keys
4. Fill in `steps.yaml`, `steps_en.yaml`, `steps_es.yaml`, `steps_dsl.yaml` with actual steps