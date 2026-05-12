module org.myjtools.openbbt.plugins.pdfreport.test {

    requires org.myjtools.openbbt.plugins.pdfreport;
    requires org.myjtools.openbbt.core;
    requires org.myjtools.openbbt.persistence;
    requires org.myjtools.imconfig;
    requires org.junit.jupiter.api;

    opens org.myjtools.plugins.pdfreport.test to org.junit.platform.commons;

}