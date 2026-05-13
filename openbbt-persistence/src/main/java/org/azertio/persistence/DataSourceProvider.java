package org.azertio.persistence;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.jooq.SQLDialect;
import javax.sql.DataSource;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DataSourceProvider {


	public enum DatabaseType {
		HSQLDB("hsqldb", SQLDialect.HSQLDB),
		POSTGRESQL("postgresql", SQLDialect.POSTGRES);

		private final String migrationFolder;
		private SQLDialect dialect;

		DatabaseType(String migrationFolder, SQLDialect dialect) {
			this.migrationFolder = migrationFolder;
			this.dialect = dialect;
		}

		public SQLDialect dialect() {
			return this.dialect;
		}
	}

	interface JdbcUrlProvider {
		String jdbcUrl();
		String username();
		String password();
		DatabaseType databaseType();
	}

	public static class HsqldbFileDataSource implements JdbcUrlProvider {

		private final Path file;

		public HsqldbFileDataSource(Path file) {
			this.file = file;
		}

		@Override
		public String jdbcUrl() {
			var url = "jdbc:hsqldb:file:" + file.toAbsolutePath() + ";DB_CLOSE_DELAY=-1;MODE=PostgreSQL";
			url += ";hsqldb.default_table_type=cached;hsqldb.cache_rows=2000;hsqldb.cache_size=1000";
			url += ";hsqldb.lock_file=false";
			return url;
		}

		@Override
		public String username() {
			return "sa";
		}

		@Override
		public String password() {
			return "";
		}

		@Override
		public DatabaseType databaseType() {
			return DatabaseType.HSQLDB;
		}
	}



	public static class PostgresqlDataSource implements JdbcUrlProvider {

		private final String jdbcUrl;
		private final String username;
		private final String password;

		public PostgresqlDataSource(String jdbcUrl, String username, String password) {
			this.jdbcUrl = jdbcUrl;
			this.username = username;
			this.password = password;
		}

		@Override
		public String jdbcUrl() {
			return jdbcUrl;
		}
		@Override
		public String username() {
			return username;
		}
		@Override
		public String password() {
			return password;
		}
		@Override
		public DatabaseType databaseType() {
			return DatabaseType.POSTGRESQL;
		}
	}


	public static DataSourceProvider hsqldb(Path file) {
		return new DataSourceProvider(new HsqldbFileDataSource(file));
	}



	public static DataSourceProvider postgresql(String jdbcUrl, String username, String password) {
		return new DataSourceProvider(new PostgresqlDataSource(jdbcUrl, username, password));
	}


	DataSourceProvider(JdbcUrlProvider jdbcUrlProvider) {
		this.jdbcUrlProvider = jdbcUrlProvider;
	}


	private final JdbcUrlProvider jdbcUrlProvider;


	public DataSource obtainDataSource() {

		HikariConfig config = new HikariConfig();
		config.setJdbcUrl(jdbcUrlProvider.jdbcUrl());
		config.setUsername(jdbcUrlProvider.username());
		config.setPassword(jdbcUrlProvider.password());
		config.setMaximumPoolSize(10);
		DataSource dataSource = new HikariDataSource(config);

		String migrationLocation = "classpath:org/myjtools/azertio/persistence/migration/"
			+ jdbcUrlProvider.databaseType().migrationFolder;

		Flyway flyway = Flyway.configure(getClass().getClassLoader())
			.dataSource(dataSource)
			.locations(migrationLocation)
			.load();
		flyway.migrate();

		return dataSource;
	}

	public SQLDialect dialect() {
		return jdbcUrlProvider.databaseType().dialect();
	}

	/**
	 * Opens a direct JDBC connection without HikariCP pool or Flyway migration.
	 * For HSQLDB, opens in read-only mode (no lock file, no transaction log) for faster startup.
	 * Intended for read-only operations where fast startup is required.
	 */
	public Connection openConnection() throws SQLException {
		String url = jdbcUrlProvider.jdbcUrl();
		if (jdbcUrlProvider.databaseType() == DatabaseType.HSQLDB) {
			url += ";readonly=true";
		}
		return DriverManager.getConnection(url, jdbcUrlProvider.username(), jdbcUrlProvider.password());
	}



}
