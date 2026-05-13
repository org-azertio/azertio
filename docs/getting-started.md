# Getting Started with Azertio

This guide walks you through installing Azertio, configuring your first testProject, installing plugins, and generating a test testPlan.

## Requirements

- **Java 21** or higher (`java -version` to check)
- Internet access to download plugins from Maven Central (or a configured Maven repository)

---

## 1. Installation

### Download

Download the latest distribution ZIP from the [releases page](https://github.com/org-myjtools/azertio/releases) and extract it to a directory of your choice:

```
azertio-<version>/
├── bin/
│   ├── azertio        # Unix/macOS launcher
│   └── azertio.bat    # Windows launcher
└── lib/               # Runtime JARs (do not modify)
```

### Build from source

```bash
git clone https://github.com/org-myjtools/azertio.git
cd azertio
mvn package -pl azertio-cli -am -DskipTests
```

The distribution ZIP will be at `azertio-cli/target/azertio-cli-<version>-dist.zip`. Extract it to the directory of your choice.

---

## 2. Add the executable to PATH

### Linux / macOS

```bash
# Replace with your actual installation path
export AZERTIO_HOME=/opt/azertio-1.0.0-alpha1
export PATH="$AZERTIO_HOME/bin:$PATH"
```

To make this permanent, add those two lines to your shell profile (`~/.bashrc`, `~/.zshrc`, or `~/.profile`) and reload it:

```bash
source ~/.bashrc   # or source ~/.zshrc
```

Verify:

```bash
azertio --help
```

### Windows

1. Open **Start → Search → "Edit the system environment variables"**
2. Click **Environment Variables…**
3. Under *System variables*, select `Path` and click **Edit**
4. Click **New** and add the full path to the `bin\` folder, e.g. `C:\tools\azertio-1.0.0-alpha1\bin`
5. Click **OK** on every dialog

Open a new Command Prompt and verify:

```cmd
azertio --help
```

---

## 3. Create the testProject configuration file

Azertio is driven by an `azertio.yaml` file that you place in your testProject's working directory. The default name is `azertio.yaml`; you can override it with the `-f` flag.

### Minimal example

```yaml
testProject:
  organization: Acme Corp
  name: My Project
  test-suites:
    - name: smoke
      tag-expression: "smoke"
    - name: regression
      tag-expression: "regression"

plugins:
  - gherkin

configuration:
  core.resourcePath: src/test/resources/features
```

### Full reference

```yaml
testProject:
  organization: string          # Organisation name
  name: string                  # Project name
  description: string           # Optional description
  test-suites:
    - name: string              # Suite name (used with -s flag)
      description: string       # Optional description
      tag-expression: string    # Tag filter, e.g. "@smoke and not @wip"

plugins:
  - gherkin                     # Short name (resolves to org.azertio.plugins:gherkin-azertio-plugin)
  - org.example:my-plugin       # Or full group:artifact coordinate
  - org.example:my-plugin:1.2.0 with com.h2database:h2-2.2.224  # With a runtime dependency

configuration:
  core.resourcePath: path/to/features           # Where test resources live (relative to CWD)
  core.environmentPath: .azertio                # Plugin/data cache directory (default: .azertio)
  core.artifacts.local.repository: ~/.m2/repository  # Local Maven cache (default: ~/.m2/repository)
  core.artifacts.repository.url: https://repo.example.com/maven2  # Custom Maven repo (optional)
  core.artifacts.repository.username: user      # Credentials for custom repo (optional)
  core.artifacts.repository.password: secret
  core.persistence.mode: in-memory             # in-memory | file | remote (default: in-memory)
  core.persistence.file: .azertio/data/db      # Path when mode=file
  core.idTagPattern: "ID-(\\w+)"               # Regex to extract IDs from Gherkin tags (optional)
  core.definitionTag: definition               # Tag marking a node as a definition (optional)
  core.implementationTag: implementation       # Tag marking a node as an implementation (optional)

profiles:                       # Named sets of placeholder values
  staging:
    env: staging
    base-url: https://staging.example.com
  production:
    env: production
    base-url: https://example.com
```

**Placeholder substitution** — use `{{key}}` in any configuration value and it will be replaced by the active profile's value:

```yaml
configuration:
  base-url: "{{base-url}}/api"
```

Activate a profile with the `-p` flag:

```bash
azertio testPlan -p staging -s smoke
```

---

## 4. Install plugins

Plugins provide components such as the test format support (e.g., Gherkin), executable steps, hooks, and reports,
among other things. Run the `install` command once per testProject, or whenever you add a new plugin to `azertio.yaml`.
Artifacts are downloaded from Maven Central and cached locally under the `core.environmentPath` directory
(`.azertio/` by default).

```bash
# Install all plugins declared in azertio.yaml
azertio install
```

```bash
# Re-install from scratch (removes cached plugins first)
azertio install --clean
```

### Runtime dependencies

Some plugins require additional JARs at runtime that are not bundled with the plugin itself — for example, a JDBC driver chosen by the user. You can declare these inline in `azertio.yaml` using the `with` keyword:

```yaml
plugins:
  - org.example:my-plugin:1.2.0 with com.h2database:h2-2.2.224
```

The artifact identifier after `with` must be `groupId:artifactId-version` (e.g. `com.h2database:h2-2.2.224`). Multiple runtime dependencies can be declared separated by commas:

```yaml
plugins:
  - org.example:my-plugin:1.2.0 with com.h2database:h2-2.2.224,org.postgresql:postgresql-42.7.3
```

Runtime dependencies are downloaded from the same Maven repository as the plugin itself and stored under `.azertio/plugins/artifacts/`. They are loaded into the plugin's module layer when the plugin is activated.

### What gets installed

```
.azertio/
└── plugins/
    ├── manifests/     # Plugin descriptors (YAML), including *.runtime.yaml for runtime deps
    └── artifacts/     # Downloaded JARs, grouped by Maven coordinates
```

---

## 5. Generate a test testPlan

The `testPlan` command loads the plugins, reads your test resources, and assembles a test testPlan. On success it prints a testPlan identifier (a ULID) to stdout.

```bash
# Generate testPlan for one suite
azertio testPlan -s smoke
```

```bash
# Generate testPlan for multiple suites
azertio testPlan -s smoke -s regression
```

```bash
# Show the full testPlan tree (scenarios, steps, …)
azertio testPlan -s smoke --detail
```

```bash
# Pass extra parameters at runtime
azertio testPlan -s smoke -D base-url=https://staging.example.com
```

```bash
# Use a profile
azertio testPlan -s smoke -p staging
```

---

## 6. Command reference

All commands share a common set of global options:

| Option | Short | Description |
|--------|-------|-------------|
| `--file` | `-f` | Path to the configuration file (default: `azertio.yaml`) |
| `--suite` | `-s` | Test suite name; may be repeated for multiple suites |
| `--profile` | `-p` | Activate a named profile defined in `azertio.yaml` |
| `-D key=value` | | Override any configuration key at runtime |
| `--debug` | `-d` | Enable verbose debug logging |
| `--help` | | Print help for the command |

### `install`

Install the plugins declared in `azertio.yaml`.

```bash
azertio install [-f <file>] [--clean]
```

| Option | Description |
|--------|-------------|
| `--clean` / `-c` | Delete existing plugin cache before installing |

### `testPlan`

Assemble and display the test testPlan.

```bash
azertio testPlan [-f <file>] [-s <suite>]... [--detail] [-p <profile>] [-D key=value]...
```

| Option | Description |
|--------|-------------|
| `--detail` | Print the full testPlan tree (suites, scenarios, steps) |

### `show-config`

Display all resolved configuration values and available options.

```bash
azertio show-config [-f <file>] [-p <profile>] [-D key=value]...
```

### `purge`

Delete all local Azertio data (plugins, cache, persisted plans).

```bash
azertio purge [-f <file>]
```

> **Warning:** This removes the entire `.azertio/` directory. You will need to run `install` again afterwards.

---

## 7. Typical workflow

```bash
# 1. Create your azertio.yaml in the testProject root
vim azertio.yaml

# 2. Install plugins (once, or when azertio.yaml changes)
azertio install

# 3. Inspect resolved configuration
azertio show-config 

# 4. Generate the test testPlan for a suite
azertio testPlan-s smoke

# 5. Inspect testPlan details
azertio testPlan -s smoke --detail

# 6. Use a profile for environment-specific values
azertio testPlan -s smoke -p staging
```

---

## Troubleshooting

| Symptom | Likely cause | Fix |
|---------|--------------|-----|
| `No SuiteAssembler found` | Plugin not installed or incompatible | Run `azertio install` and verify the plugin name in `azertio.yaml` |
| `Test suite 'X' not found` | Suite name mismatch | Check the `test-suites[].name` values in `azertio.yaml` |
| `Failed to read configuration file` | Wrong path or missing file | Pass the correct path with `-f` |
| `No test testPlan nodes assembled` | Tag expression matches nothing | Verify the `tag-expression` and that resources exist at `core.resourcePath` |
| Artifact download fails | Network or repository config | Check `core.artifacts.repository.url` and proxy settings |

Enable debug logging with `-d` to get detailed output for any issue:

```bash
azertio testPlan -f azertio.yaml -s smoke -d
```
