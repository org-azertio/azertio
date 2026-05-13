package org.azertio.persistence.test.plan;

import org.junit.jupiter.api.io.TempDir;
import org.azertio.persistence.DataSourceProvider;
import java.nio.file.Path;

class HsqldbFileRepositoryTest extends AbstractRepositoryTest {

	@TempDir
	private Path tempDir;

	@Override
	protected DataSourceProvider dataSourceProvider() {
		return DataSourceProvider.hsqldb(tempDir.resolve("testdb"));
	}

}