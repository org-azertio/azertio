module org.azertio.plugins.htmlreport.test {

    requires org.azertio.plugins.htmlreport;
    requires org.azertio.core;
    requires org.azertio.persistence;
    requires org.myjtools.imconfig;
    requires org.junit.jupiter.api;
    requires org.assertj.core;
    requires java.desktop;

    opens org.azertio.plugins.htmlreport.test to org.junit.platform.commons;

}