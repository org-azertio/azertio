package org.azertio.persistence.test.plan;

import org.junit.jupiter.api.condition.EnabledIf;
import org.azertio.persistence.DataSourceProvider;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@EnabledIf("isDockerAvailable")
class PostgresqlRepositoryTest extends AbstractRepositoryTest {

	static boolean isDockerAvailable() {
		try {
			DockerClientFactory.instance().client();
			return true;
		} catch (Throwable ex) {
			return false;
		}
	}

	@Container
	private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

	@Override
	protected DataSourceProvider dataSourceProvider() {
		return DataSourceProvider.postgresql(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
	}

}