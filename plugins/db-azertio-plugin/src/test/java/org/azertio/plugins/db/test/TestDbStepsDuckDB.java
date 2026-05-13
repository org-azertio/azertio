package org.azertio.plugins.db.test;

class TestDbStepsDuckDB extends AbstractDbStepsTest {

    private static final String DB_PATH = System.getProperty("java.io.tmpdir") + "/azertio_test_duckdb.duckdb";

    @Override protected String jdbcUrl()  { return "jdbc:duckdb:" + DB_PATH; }
    @Override protected String username() { return ""; }
    @Override protected String password() { return ""; }
    @Override protected String dialect()  { return "DUCKDB"; }
    @Override protected String driver()   { return "org.duckdb.DuckDBDriver"; }
    @Override protected boolean quoteIdentifiers() { return false; }

}