module org.azertio.plugins.db.test {

    requires org.azertio.plugins.db;
    requires org.azertio.plugins.gherkin;
    requires org.azertio.persistence;
    requires org.azertio.core;
    requires org.myjtools.imconfig;
    requires azertio.test.support;
    requires org.junit.jupiter.api;
    requires java.sql;
    requires com.h2database;
    requires org.hsqldb;
    requires org.xerial.sqlitejdbc;
    requires duckdb.jdbc;
    requires org.apache.poi.poi;
    requires org.apache.poi.ooxml;

    opens org.azertio.plugins.db.test to org.junit.platform.commons;

}
