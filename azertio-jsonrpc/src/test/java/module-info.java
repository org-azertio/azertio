module org.azertio.jsonrpc.test {
    requires org.azertio.core;
    requires org.azertio.jsonrpc;
    requires com.google.gson;
    requires org.junit.jupiter.api;
    requires org.assertj.core;
    opens org.azertio.jsonrpc.serve.test to org.junit.platform.commons;
}