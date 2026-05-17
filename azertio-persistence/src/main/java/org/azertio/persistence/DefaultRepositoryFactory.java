package org.azertio.persistence;

import org.azertio.core.AzertioException;
import org.azertio.core.contributors.RepositoryFactory;
import org.azertio.core.persistence.AttachmentRepository;
import org.azertio.core.persistence.Repository;
import org.azertio.core.persistence.TestExecutionRepository;
import org.azertio.core.persistence.TestPlanRepository;
import org.azertio.core.util.FileUtil;
import org.azertio.persistence.attachment.LocalAttachmentRepository;
import org.azertio.persistence.attachment.MinioAttachmentRepository;
import org.azertio.persistence.execution.JooqExecutionRepository;
import org.azertio.persistence.plan.JooqPlanRepository;
import org.myjtools.imconfig.Config;
import org.myjtools.jexten.Extension;
import org.myjtools.jexten.Inject;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.List;
import static org.azertio.core.AzertioConfig.*;

@Extension
public class DefaultRepositoryFactory implements RepositoryFactory {

	@Inject
	Config config;


	@Override
	@SuppressWarnings("unchecked")
	public <T extends Repository> T createRepository(Class<T> type) {
		String mode = config.get(PERSISTENCE_MODE, String.class).orElse(PERSISTENCE_MODE_TRANSIENT);
		return switch (mode) {
			case PERSISTENCE_MODE_TRANSIENT -> {
				try {
					if (type.equals(AttachmentRepository.class)) {
						yield (T) new LocalAttachmentRepository(FileUtil.createSafeTempDirectory("azertio-attachments"));
					}
					yield (T) createFileRepository(type, FileUtil.createSafeTempFile("azertio", "db"));
				} catch (IOException e) {
					throw new AzertioException(e);
				}
			}
			case PERSISTENCE_MODE_FILE -> {
				Path envPath = config.get(ENV_PATH, Path::of).orElseThrow(
					() -> new AzertioException("Repository environment path not configured: {}", ENV_PATH)
				);
				if (type.equals(AttachmentRepository.class)) {
					yield (T) new LocalAttachmentRepository(envPath.resolve("attachments"));
				}
				Path filePath = config.get(PERSISTENCE_FILE, Path::of).orElseThrow(
					() -> new AzertioException("Repository file path not configured: {}", PERSISTENCE_FILE)
				);
				yield (T) createFileRepository(type, envPath.resolve(filePath));
			}
			case PERSISTENCE_MODE_REMOTE -> {
				if (type.equals(AttachmentRepository.class)) {
					yield (T) createMinioRepository();
				}
				String url = config.get(PERSISTENCE_DB_URL, String::toString).orElseThrow(
					() -> new AzertioException("Repository remote URL not configured: {}", PERSISTENCE_DB_URL)
				);
				String username = config.get(PERSISTENCE_DB_USERNAME, String::toString).orElseThrow(
					() -> new AzertioException("Repository remote username not configured: {}", PERSISTENCE_DB_USERNAME)
				);
				String password = config.get(PERSISTENCE_DB_PASSWORD, String::toString).orElseThrow(
					() -> new AzertioException("Repository remote password not configured: {}", PERSISTENCE_DB_PASSWORD)
				);
				yield (T) createRemoteRepository(type, url, username, password);
			}
			default -> throw new AzertioException("Unsupported repository mode: {}, expected: {}",
				mode,
				List.of(PERSISTENCE_MODE_FILE, PERSISTENCE_MODE_TRANSIENT, PERSISTENCE_MODE_REMOTE)
			);
		};
	}


	@Override
	@SuppressWarnings("unchecked")
	public <T extends Repository> T createReadOnlyRepository(Class<T> type) {
		String mode = config.get(PERSISTENCE_MODE, String.class).orElse(PERSISTENCE_MODE_FILE);
		try {
			DataSourceProvider provider = switch (mode) {
				case PERSISTENCE_MODE_FILE -> {
					Path envPath = config.get(ENV_PATH, Path::of).orElseThrow(
						() -> new AzertioException("Repository environment path not configured: {}", ENV_PATH)
					);
					Path filePath = config.get(PERSISTENCE_FILE, Path::of).orElseThrow(
						() -> new AzertioException("Repository file path not configured: {}", PERSISTENCE_FILE)
					);
					yield DataSourceProvider.h2file(envPath.resolve(filePath));
				}
				case PERSISTENCE_MODE_REMOTE -> {
					String url = config.get(PERSISTENCE_DB_URL, String::toString).orElseThrow(
						() -> new AzertioException("Repository remote URL not configured: {}", PERSISTENCE_DB_URL)
					);
					String username = config.get(PERSISTENCE_DB_USERNAME, String::toString).orElseThrow(
						() -> new AzertioException("Repository remote username not configured: {}", PERSISTENCE_DB_USERNAME)
					);
					String password = config.get(PERSISTENCE_DB_PASSWORD, String::toString).orElseThrow(
						() -> new AzertioException("Repository remote password not configured: {}", PERSISTENCE_DB_PASSWORD)
					);
					yield DataSourceProvider.postgresql(url, username, password);
				}
				default -> throw new AzertioException("Unsupported repository mode for read-only access: {}", mode);
			};
			Connection connection = provider.openConnection();
			if (type.equals(TestPlanRepository.class)) {
				return (T) new JooqPlanRepository(connection, provider.dialect());
			}
			if (type.equals(TestExecutionRepository.class)) {
				return (T) new JooqExecutionRepository(connection, provider.dialect());
			}
			throw new AzertioException("Unsupported repository type: {}", type.getName());
		} catch (AzertioException e) {
			throw e;
		} catch (Exception e) {
			throw new AzertioException(e, "Failed to open read-only repository connection");
		}
	}


	private MinioAttachmentRepository createMinioRepository() {
		String url = config.get(ATTACHMENT_SERVER_URL, String::toString).orElseThrow(
			() -> new AzertioException("Attachment server URL not configured: {}", ATTACHMENT_SERVER_URL)
		);
		String username = config.get(ATTACHMENT_SERVER_USERNAME, String::toString).orElseThrow(
			() -> new AzertioException("Attachment server username not configured: {}", ATTACHMENT_SERVER_USERNAME)
		);
		String password = config.get(ATTACHMENT_SERVER_PASSWORD, String::toString).orElseThrow(
			() -> new AzertioException("Attachment server password not configured: {}", ATTACHMENT_SERVER_PASSWORD)
		);
		return new MinioAttachmentRepository(url, username, password);
	}


	private static Object createRemoteRepository(Class<?> type, String url, String username, String password) {
		DataSourceProvider provider = DataSourceProvider.postgresql(url, username, password);
		if (type.equals(TestPlanRepository.class)) {
			return new JooqPlanRepository(provider);
		}
		if (type.equals(TestExecutionRepository.class)) {
			return new JooqExecutionRepository(provider);
		}
		throw new AzertioException("Unsupported repository type for remote mode: {}", type.getName());
	}


	private static Object createFileRepository(Class<?> type, Path filePath) {
		DataSourceProvider provider = DataSourceProvider.h2file(filePath);
		if (type.equals(TestPlanRepository.class)) {
			return new JooqPlanRepository(provider);
		}
		if (type.equals(TestExecutionRepository.class)) {
			return new JooqExecutionRepository(provider);
		}
		throw new AzertioException("Unsupported repository type for file mode: {}", type.getName());
	}

}
