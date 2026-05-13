package org.azertio.plugins.db.test;

class TestDbStepsHsqldb extends AbstractDbStepsTest {

    @Override protected String jdbcUrl()  { return "jdbc:hsqldb:mem:testdb"; }
    @Override protected String username() { return "SA"; }
    @Override protected String password() { return ""; }
    @Override protected String dialect()  { return "HSQLDB"; }
    @Override protected String driver()   { return "org.hsqldb.jdbc.JDBCDriver"; }
    @Override protected boolean quoteIdentifiers() { return false; }

}