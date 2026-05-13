module org.azertio.cli {
	exports org.azertio.cli;
	requires org.azertio.core;
	requires org.azertio.persistence;
	requires info.picocli;
	requires org.myjtools.jexten.plugin;
	requires com.google.common;
	requires org.myjtools.imconfig;
	requires org.slf4j;
	requires ch.qos.logback.classic;
	requires ch.qos.logback.core;
requires org.azertio.lsp;
	requires org.azertio.jsonrpc;
	requires com.google.gson;

	opens org.azertio.cli to info.picocli;

}