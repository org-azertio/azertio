package org.azertio.persistence.test.attachment;

import org.junit.jupiter.api.condition.EnabledIf;
import org.azertio.core.persistence.AttachmentRepository;
import org.azertio.persistence.attachment.MinioAttachmentRepository;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.time.Duration;

@Testcontainers
@EnabledIf("isDockerAvailable")
class MinioAttachmentRepositoryTest extends AbstractAttachmentRepositoryTest {

	static boolean isDockerAvailable() {
		try {
			DockerClientFactory.instance().client();
			return true;
		} catch (Throwable ex) {
			return false;
		}
	}

	private static final String ACCESS_KEY = "minioadmin";
	private static final String SECRET_KEY = "minioadmin";

	@Container
	@SuppressWarnings("resource")
	private static final GenericContainer<?> minio = new GenericContainer<>("minio/minio:latest")
		.withExposedPorts(9000)
		.withEnv("MINIO_ROOT_USER", ACCESS_KEY)
		.withEnv("MINIO_ROOT_PASSWORD", SECRET_KEY)
		.withCommand("server", "/data")
		.waitingFor(new HttpWaitStrategy()
			.forPort(9000)
			.forPath("/minio/health/live")
			.withStartupTimeout(Duration.ofSeconds(60)));

	@Override
	protected AttachmentRepository repository() {
		String url = "http://" + minio.getHost() + ":" + minio.getMappedPort(9000);
		return new MinioAttachmentRepository(url, ACCESS_KEY, SECRET_KEY);
	}

}
