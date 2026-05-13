module org.azertio.lsp.test {
    requires org.azertio.lsp;
    requires org.azertio.core;
    requires org.myjtools.gherkinparser;
    requires org.myjtools.imconfig;
    requires org.eclipse.lsp4j;
    requires org.eclipse.lsp4j.jsonrpc;
    requires org.junit.jupiter.api;
    requires org.assertj.core;
    opens org.azertio.lsp.test to org.junit.platform.commons;
}