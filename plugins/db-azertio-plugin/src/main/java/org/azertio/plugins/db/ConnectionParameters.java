package org.azertio.plugins.db;

public record ConnectionParameters(
	String url,
	String username,
	String password,
	String driver,
	String schema,
	String catalog,
	String dialect,
	boolean quoteIdentifiers
) {
}
