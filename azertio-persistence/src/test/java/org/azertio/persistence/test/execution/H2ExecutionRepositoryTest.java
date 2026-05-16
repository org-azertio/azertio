package org.azertio.persistence.test.execution;

import org.junit.jupiter.api.io.TempDir;
import org.azertio.persistence.DataSourceProvider;
import java.nio.file.Path;

class H2ExecutionRepositoryTest extends AbstractExecutionRepositoryTest {

	@TempDir
	private Path tempDir;

	@Override
	protected DataSourceProvider dataSourceProvider() {
		return DataSourceProvider.h2fileLocal(tempDir.resolve("testdb"));
	}

}