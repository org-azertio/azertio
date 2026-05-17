package org.azertio.core;

import org.myjtools.jexten.Extension;
import org.azertio.core.contributors.ConfigProvider;
import java.nio.file.Path;

/**
 * @author Luis Iñesta Gelabert - luiinge@gmail.com
 */
@Extension
public class AzertioConfig extends ConfigAdapter implements ConfigProvider {

	public static final String RESOURCE_PATH = "core.resourcePath";
	public static final String RESOURCE_FILTER = "core.resourceFilter";
	public static final String ENV_PATH = "core.environmentPath";
	public static final String TIME_ZONE = "core.timeZone";

	public static final String STEP_EXECUTION_TIMEOUT = "core.stepExecutionTimeout";

	/** Configuration key for the regex pattern used to extract identifiers from Gherkin tags. */
	public static final String ID_TAG_PATTERN = "core.idTagPattern";

	/** Configuration key for the tag that marks a feature as a definition. */
	public static final String DEFINITION_TAG = "core.definitionTag";

	/** Configuration key for the tag that marks a feature as an implementation. */
	public static final String IMPLEMENTATION_TAG = "core.implementationTag";

	/** Configuration key for the tag that marks a test case as eligible for parallel execution. */
	public static final String PARALLEL_EXECUTION_TAG = "core.parallelExecutionTag";

	public static final String PERSISTENCE_MODE = "core.persistence.mode";
	public static final String PERSISTENCE_MODE_TRANSIENT = "transient";
	public static final String PERSISTENCE_MODE_FILE = "file";
	public static final String PERSISTENCE_MODE_REMOTE = "remote";
	public static final String PERSISTENCE_FILE = "core.persistence.file";
	public static final String PERSISTENCE_DB_URL = "core.persistence.db.url";
	public static final String PERSISTENCE_DB_USERNAME = "core.persistence.db.username";
	public static final String PERSISTENCE_DB_PASSWORD = "core.persistence.db.password";

	public static final String ARTIFACTS_LOCAL_REPOSITORY = "core.artifacts.local.repository";
	public static final String ARTIFACTS_REPOSITORY_URL = "core.artifacts.repository.url";
	public static final String ARTIFACTS_REPOSITORY_USERNAME = "core.artifacts.repository.username";
	public static final String ARTIFACTS_REPOSITORY_PASSWORD = "core.artifacts.repository.password";
	public static final String ARTIFACTS_REPOSITORY_PROXY_URL = "core.artifacts.repository.proxy.url";
	public static final String ARTIFACTS_REPOSITORY_PROXY_USERNAME= "core.artifacts.repository.proxy.username";
	public static final String ARTIFACTS_REPOSITORY_PROXY_PASSWORD= "core.artifacts.repository.proxy.password";

	/** Configuration key for the URL of the remote attachment server (Minio/S3). Used when persistence mode is {@code remote}. */
	public static final String ATTACHMENT_SERVER_URL = "core.attachments.server.url";

	/** Configuration key for the username (access key) of the remote attachment server. */
	public static final String ATTACHMENT_SERVER_USERNAME = "core.attachments.server.username";

	/** Configuration key for the password (secret key) of the remote attachment server. */
	public static final String ATTACHMENT_SERVER_PASSWORD = "core.attachments.server.password";

	public static final Path ENV_DEFAULT_PATH = Path.of(".azertio");
	public static final Path PLUGINS_PATH = Path.of("plugins");

	/** {@inheritDoc} */
	@Override
	protected String resource() {
		return "core-config.yaml";
	}

}
