package org.azertio.plugins.db;

import org.myjtools.jexten.Extension;
import org.myjtools.jexten.Scope;
import org.azertio.core.ConfigAdapter;
import org.azertio.core.contributors.ConfigProvider;

@Extension(
	name = "Database Config Provider",
	extensionPointVersion = "1.0",
	scope = Scope.SINGLETON		
)
public class DbConfigProvider extends ConfigAdapter implements ConfigProvider {

	public static final String DATASOURCES = "db.datasources";
	public static final String DATASOURCE_URL = "url";
	public static final String DATASOURCE_USERNAME = "username";
	public static final String DATASOURCE_PASSWORD = "password";
	public static final String DATASOURCE_DRIVER = "driver";
	public static final String DATASOURCE_SCHEMA = "schema";
	public static final String DATASOURCE_CATALOG = "catalog";
	public static final String DATASOURCE_DIALECT = "dialect";
	public static final String DATASOURCE_QUOTE_IDENTIFIERS = "quoteIdentifiers";
	public static final String NULL_VALUE = "db.nullValue";
	public static final String MAX_ASSERT_ROWS = "db.maxAssertRows";

	@Override
	protected String resource() {
		return "config.yaml";
	}

}
