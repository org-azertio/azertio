package org.azertio.plugins.db.test;

class TestDbStepsSqlite extends AbstractDbStepsTest {

    private static final String DB_PATH = System.getProperty("java.io.tmpdir") + "/azertio_test_sqlite.db";

    @Override protected String jdbcUrl()  { return "jdbc:sqlite:" + DB_PATH; }
    @Override protected String username() { return ""; }
    @Override protected String password() { return ""; }
    @Override protected String dialect()  { return "SQLITE"; }
    @Override protected String driver()   { return "org.sqlite.JDBC"; }
    @Override protected boolean quoteIdentifiers() { return false; }

}