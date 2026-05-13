package org.azertio.plugins.db.test;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.azertio.test.FeatureDir;
import org.azertio.test.JUnitAzertioPlan;
import org.azertio.test.AzertioExtension;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(AzertioExtension.class)
abstract class AbstractDbStepsTest {

	protected abstract String jdbcUrl();
	protected abstract String username();
	protected abstract String password();
	protected abstract String dialect();
	protected abstract String driver();
	protected boolean quoteIdentifiers() { return true; }

	@BeforeAll
	void seedDatabase() throws Exception {
		try (Connection conn = DriverManager.getConnection(jdbcUrl(), username(), password());
			 Statement stmt = conn.createStatement()) {
			stmt.execute("CREATE TABLE IF NOT EXISTS users (id INT PRIMARY KEY, name VARCHAR(100))");
			stmt.execute("DELETE FROM users");
			stmt.execute("INSERT INTO users VALUES (1, 'Alice')");
			stmt.execute("INSERT INTO users VALUES (2, 'Bob')");
			stmt.execute("INSERT INTO users VALUES (3, 'Carol')");
		}
		XlsTestFixtures.create(getClass());
	}

	private JUnitAzertioPlan withDbConfig(JUnitAzertioPlan plan) {
		return plan
			.withConfig("db.datasources.test.url", jdbcUrl())
			.withConfig("db.datasources.test.username", username())
			.withConfig("db.datasources.test.password", password())
			.withConfig("db.datasources.test.driver", driver())
			.withConfig("db.datasources.test.dialect", dialect())
			.withConfig("db.datasources.test.quoteIdentifiers", String.valueOf(quoteIdentifiers()));
	}

	@Test
	@FeatureDir("count-passes")
	void count_passes(JUnitAzertioPlan plan) {
		withDbConfig(plan).execute().assertAllPassed();
	}

	@Test
	@FeatureDir("count-fails")
	void count_fails(JUnitAzertioPlan plan) {
		withDbConfig(plan).execute().assertAllFailed();
	}

	@Test
	@FeatureDir("dsl-count-passes")
	void dsl_count_passes(JUnitAzertioPlan plan) {
		withDbConfig(plan).execute().assertAllPassed();
	}

	@Test
	@FeatureDir("dsl-count-fails")
	void dsl_count_fails(JUnitAzertioPlan plan) {
		withDbConfig(plan).execute().assertAllFailed();
	}

	@Test
	@FeatureDir("table-contains-passes")
	void table_contains_passes(JUnitAzertioPlan plan) {
		withDbConfig(plan).execute().assertAllPassed();
	}

	@Test
	@FeatureDir("table-contains-fails")
	void table_contains_fails(JUnitAzertioPlan plan) {
		withDbConfig(plan).execute().assertAllFailed();
	}

	@Test
	@FeatureDir("table-is-passes")
	void table_is_passes(JUnitAzertioPlan plan) {
		withDbConfig(plan).execute().assertAllPassed();
	}

	@Test
	@FeatureDir("table-is-fails")
	void table_is_fails(JUnitAzertioPlan plan) {
		withDbConfig(plan).execute().assertAllFailed();
	}

	@Test
	@FeatureDir("dsl-table-contains-passes")
	void dsl_table_contains_passes(JUnitAzertioPlan plan) {
		withDbConfig(plan).execute().assertAllPassed();
	}

	@Test
	@FeatureDir("dsl-table-contains-fails")
	void dsl_table_contains_fails(JUnitAzertioPlan plan) {
		withDbConfig(plan).execute().assertAllFailed();
	}

	@Test
	@FeatureDir("dsl-table-is-passes")
	void dsl_table_is_passes(JUnitAzertioPlan plan) {
		withDbConfig(plan).execute().assertAllPassed();
	}

	@Test
	@FeatureDir("dsl-table-is-fails")
	void dsl_table_is_fails(JUnitAzertioPlan plan) {
		withDbConfig(plan).execute().assertAllFailed();
	}

	@Test
	@FeatureDir("csv-table-contains-passes")
	void csv_table_contains_passes(JUnitAzertioPlan plan) {
		withDbConfig(plan).execute().assertAllPassed();
	}

	@Test
	@FeatureDir("csv-table-contains-fails")
	void csv_table_contains_fails(JUnitAzertioPlan plan) {
		withDbConfig(plan).execute().assertAllFailed();
	}

	@Test
	@FeatureDir("csv-table-is-passes")
	void csv_table_is_passes(JUnitAzertioPlan plan) {
		withDbConfig(plan).execute().assertAllPassed();
	}

	@Test
	@FeatureDir("csv-table-is-fails")
	void csv_table_is_fails(JUnitAzertioPlan plan) {
		withDbConfig(plan).execute().assertAllFailed();
	}

	@Test
	@FeatureDir("dsl-csv-table-contains-passes")
	void dsl_csv_table_contains_passes(JUnitAzertioPlan plan) {
		withDbConfig(plan).execute().assertAllPassed();
	}

	@Test
	@FeatureDir("dsl-csv-table-contains-fails")
	void dsl_csv_table_contains_fails(JUnitAzertioPlan plan) {
		withDbConfig(plan).execute().assertAllFailed();
	}

	@Test
	@FeatureDir("dsl-csv-table-is-passes")
	void dsl_csv_table_is_passes(JUnitAzertioPlan plan) {
		withDbConfig(plan).execute().assertAllPassed();
	}

	@Test
	@FeatureDir("dsl-csv-table-is-fails")
	void dsl_csv_table_is_fails(JUnitAzertioPlan plan) {
		withDbConfig(plan).execute().assertAllFailed();
	}

	@Test
	@FeatureDir("xls-contains-passes")
	void xls_contains_passes(JUnitAzertioPlan plan) {
		withDbConfig(plan).execute().assertAllPassed();
	}

	@Test
	@FeatureDir("xls-contains-fails")
	void xls_contains_fails(JUnitAzertioPlan plan) {
		withDbConfig(plan).execute().assertAllFailed();
	}

	@Test
	@FeatureDir("dsl-xls-contains-passes")
	void dsl_xls_contains_passes(JUnitAzertioPlan plan) {
		withDbConfig(plan).execute().assertAllPassed();
	}

	@Test
	@FeatureDir("dsl-xls-contains-fails")
	void dsl_xls_contains_fails(JUnitAzertioPlan plan) {
		withDbConfig(plan).execute().assertAllFailed();
	}

	@Test
	@FeatureDir("query-count-passes")
	void query_count_passes(JUnitAzertioPlan plan) {
		withDbConfig(plan).execute().assertAllPassed();
	}

	@Test
	@FeatureDir("query-count-fails")
	void query_count_fails(JUnitAzertioPlan plan) {
		withDbConfig(plan).execute().assertAllFailed();
	}

	@Test
	@FeatureDir("dsl-query-count-passes")
	void dsl_query_count_passes(JUnitAzertioPlan plan) {
		withDbConfig(plan).execute().assertAllPassed();
	}

	@Test
	@FeatureDir("dsl-query-count-fails")
	void dsl_query_count_fails(JUnitAzertioPlan plan) {
		withDbConfig(plan).execute().assertAllFailed();
	}

	@Test
	@FeatureDir("query-dml-count-passes")
	void query_dml_count_passes(JUnitAzertioPlan plan) {
		withDbConfig(plan).execute().assertAllPassed();
	}

	@Test
	@FeatureDir("query-dml-count-fails")
	void query_dml_count_fails(JUnitAzertioPlan plan) {
		withDbConfig(plan).execute().assertAllFailed();
	}

	@Test
	@FeatureDir("dsl-query-dml-count-passes")
	void dsl_query_dml_count_passes(JUnitAzertioPlan plan) {
		withDbConfig(plan).execute().assertAllPassed();
	}

	@Test
	@FeatureDir("dsl-query-dml-count-fails")
	void dsl_query_dml_count_fails(JUnitAzertioPlan plan) {
		withDbConfig(plan).execute().assertAllFailed();
	}

	@Test
	@FeatureDir("query-store-result-passes")
	void query_store_result_passes(JUnitAzertioPlan plan) {
		withDbConfig(plan).execute().assertAllPassed();
	}

	@Test
	@FeatureDir("dsl-query-store-result-passes")
	void dsl_query_store_result_passes(JUnitAzertioPlan plan) {
		withDbConfig(plan).execute().assertAllPassed();
	}

	@Test
	@FeatureDir("query-teardown-passes")
	void query_teardown_passes(JUnitAzertioPlan plan) {
		withDbConfig(plan).execute().assertAllPassed();
	}

	@Test
	@FeatureDir("dsl-query-teardown-passes")
	void dsl_query_teardown_passes(JUnitAzertioPlan plan) {
		withDbConfig(plan).execute().assertAllPassed();
	}

}