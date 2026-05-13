module org.azertio.plugins.pdfreport.test {

    requires org.azertio.plugins.pdfreport;
    requires org.azertio.core;
    requires org.azertio.persistence;
    requires org.myjtools.imconfig;
    requires org.junit.jupiter.api;
    requires org.assertj.core;
    requires java.desktop;

    opens org.myjtools.plugins.pdfreport.test to org.junit.platform.commons;

}