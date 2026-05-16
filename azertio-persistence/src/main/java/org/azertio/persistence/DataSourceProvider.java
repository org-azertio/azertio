package org.azertio.persistence;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.jooq.SQLDialect;
import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DataSourceProvider {


	public enum DatabaseType {
		H2("h2", SQLDialect.H2),
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

	public static class H2FileDataSource implements JdbcUrlProvider {

		private final Path file;
		private final boolean autoServer;

		public H2FileDataSource(Path file, boolean autoServer) {
			this.file = file;
			this.autoServer = autoServer;
		}

		@Override
		public String jdbcUrl() {
			var url = "jdbc:h2:file:" + file.toAbsolutePath()
				+ ";MODE=PostgreSQL;NON_KEYWORDS=KEY,VALUE";
			if (autoServer) {
				url += ";AUTO_SERVER=TRUE";
			} else {
				url += ";DB_CLOSE_ON_EXIT=FALSE";
			}
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
			return DatabaseType.H2;
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


	public static DataSourceProvider h2file(Path file) {
		return new DataSourceProvider(new H2FileDataSource(file, true));
	}

	public static DataSourceProvider h2fileLocal(Path file) {
		return new DataSourceProvider(new H2FileDataSource(file, false));
	}



	public static DataSourceProvider postgresql(String jdbcUrl, String username, String password) {
		return new DataSourceProvider(new PostgresqlDataSource(jdbcUrl, username, password));
	}


	DataSourceProvider(JdbcUrlProvider jdbcUrlProvider) {
		this.jdbcUrlProvider = jdbcUrlProvider;
	}


	private final JdbcUrlProvider jdbcUrlProvider;


	public DataSource obtainDataSource() {

		if (jdbcUrlProvider instanceof H2FileDataSource h2 && h2.file.getParent() != null) {
			try {
				Files.createDirectories(h2.file.getParent());
			} catch (IOException e) {
				throw new AzertioException(e, "Cannot create database directory: {}", h2.file.getParent());
			}
		}

		HikariConfig config = new HikariConfig();
		config.setJdbcUrl(jdbcUrlProvider.jdbcUrl());
		config.setUsername(jdbcUrlProvider.username());
		config.setPassword(jdbcUrlProvider.password());
		config.setMaximumPoolSize(10);
		DataSource dataSource = new HikariDataSource(config);

		String migrationLocation = "classpath:org/azertio/persistence/migration/"
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
	 * Intended for read-only operations where fast startup is required.
	 */
	public Connection openConnection() throws SQLException {
		return DriverManager.getConnection(jdbcUrlProvider.jdbcUrl(), jdbcUrlProvider.username(), jdbcUrlProvider.password());
	}



}
