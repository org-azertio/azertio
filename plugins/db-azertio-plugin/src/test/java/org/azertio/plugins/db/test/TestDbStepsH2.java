package org.azertio.plugins.db.test;

class TestDbStepsH2 extends AbstractDbStepsTest {

    @Override protected String jdbcUrl()  { return "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;CASE_INSENSITIVE_IDENTIFIERS=TRUE"; }
    @Override protected String username() { return "sa"; }
    @Override protected String password() { return "sa"; }
    @Override protected String dialect()  { return "H2"; }
    @Override protected String driver()   { return "org.h2.Driver"; }

}
