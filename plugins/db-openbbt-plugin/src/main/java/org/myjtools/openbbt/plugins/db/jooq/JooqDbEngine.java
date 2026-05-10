package org.myjtools.openbbt.plugins.db.jooq;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jooq.*;
import org.jooq.Record;
import org.jooq.conf.RenderQuotedNames;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;
import org.jooq.impl.DataSourceConnectionProvider;
import org.myjtools.openbbt.core.OpenBBTException;
import org.myjtools.openbbt.core.testplan.DataTable;
import org.myjtools.openbbt.core.util.Either;
import org.myjtools.openbbt.plugins.db.ConnectionParameters;
import org.myjtools.openbbt.plugins.db.DataSet;
import org.myjtools.openbbt.plugins.db.DbEngine;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JooqDbEngine implements DbEngine, AutoCloseable {

	private final Map<String, DSLContext> dslContexts = new HashMap<>();
	private final Map<String, HikariDataSource> dataSources = new HashMap<>();
	private final String nullValue;
	private final int maxAssertRows;

	public JooqDbEngine(Map<String, ConnectionParameters> connections, String nullValue, int maxAssertRows) {
		this.nullValue = nullValue;
		this.maxAssertRows = maxAssertRows;
		for (Map.Entry<String, ConnectionParameters> entry : connections.entrySet()) {
			String alias = entry.getKey();
			ConnectionParameters params = entry.getValue();
			HikariDataSource dataSource = createDataSource(params);
			dataSources.put(alias, dataSource);
			SQLDialect dialect = SQLDialect.valueOf(params.dialect().toUpperCase());
			Settings settings = new Settings();
			if (!params.quoteIdentifiers()) {
				settings = settings.withRenderQuotedNames(RenderQuotedNames.NEVER);
			}
			dslContexts.put(alias, DSL.using(new DataSourceConnectionProvider(dataSource), dialect, settings));
		}
	}

	@Override
	public void close() {
		dataSources.values().forEach(HikariDataSource::close);
	}

	private HikariDataSource createDataSource(ConnectionParameters params) {
		HikariConfig config = new HikariConfig();
		config.setJdbcUrl(params.url());
		config.setUsername(params.username());
		config.setPassword(params.password());
		config.setMaximumPoolSize(10);
		config.setCatalog(params.catalog());
		config.setSchema(params.schema());
		config.setDriverClassName(params.driver());
		return new HikariDataSource(config);
	}

	private DSLContext dslContext(String alias) {
		DSLContext dslContext = dslContexts.get(alias);
		if (dslContext == null) {
			throw new OpenBBTException("Unknown datasource {}",alias);
		}
		return dslContext;
	}


	@Override
	public void insertDataTable(String alias, String table, DataTable data) {
		List<String> columns = data.values().getFirst();
		List<Field<Object>> fields = columns.stream().map(DSL::field).toList();
		List<List<String>> values = data.values().stream().skip(1).toList();
		dslContext(alias)
			.insertInto(DSL.table(table))
			.columns(fields)
			.values(values)
			.execute();
	}


	@Override
	public void insertCSVFile(String datasource, String table, java.nio.file.Path csvFile) {
		dslContext(datasource).loadInto(DSL.table(table)).loadCSV(csvFile.toFile());
	}

	@Override
	public void insertSubselect(String datasource, String table, String subselect) {
		dslContext(datasource)
			.insertInto(DSL.table(table))
			.select((Select<?>) DSL.resultQuery(subselect))
			.execute();
	}

	@Override
	public void insertXLSFile(String datasource, String table, java.nio.file.Path xlsFile) {
		try {
			new ExcelRecordLoader(dslContext(datasource), nullValue).loadExcel(xlsFile.toString());
		} catch (IOException e) {
			throw new OpenBBTException(e, "Failed to load Excel file {}", xlsFile);
		}
	}

	@Override
	public Integer executeCountQueryFromTable(String alias, String table) {
		return dslContext(alias).selectCount().from(DSL.table(DSL.name(table))).fetchOne(0, Integer.class);
	}

	@Override
	public String printTable(String alias, String table) {
		var result = dslContext(alias).select().from(DSL.table(DSL.name(table))).maxRows(maxAssertRows).fetch();
		return result.format();
	}


	@Override
	public void assertTableContains(String alias, String table, DataSet dataSet) {
		for (List<String> expectedRow : dataSet.rows()) {
			boolean found = rowExists(alias, table, dataSet.columns(), expectedRow);
			if (!found) {
				throw new AssertionError(
					"Expected row not found in table "+table+": "+rowDescription(dataSet.columns(), expectedRow)
				);
			}
		}
	}


	@Override
	public void assertTableIs(String alias, String table, DataSet dataSet) {
		int actualCount = executeCountQueryFromTable(alias, table);
		if (actualCount != dataSet.rows().size()) {
			throw new AssertionError(
				"Table " + table + " has " + actualCount + " rows but expected " + dataSet.rows().size()
			);
		}
		for (List<String> expectedRow : dataSet.rows()) {
			boolean found = rowExists(alias, table, dataSet.columns(), expectedRow);
			if (!found) {
				throw new AssertionError(
					"Expected row not found in table " + table + ": " + rowDescription(dataSet.columns(), expectedRow)
				);
			}
		}
	}


	@Override
	public DataSet readTable(String table, DataTable dataTable) {
		return new DataTableRecordLoader(maxAssertRows).load(table, dataTable);
	}


	@Override
	public DataSet readCsv(String table, Path csvFile) {
		return new CsvRecordLoader(nullValue, maxAssertRows).load(table, csvFile);
	}


	@Override
	public List<DataSet> readXls(Path file) {
		try {
			return new ExcelRecordLoader(nullValue).readExcel(file, maxAssertRows);
		} catch (IOException e) {
			throw new OpenBBTException(e, "Failed to read Excel file {}", file);
		}
	}


	@Override
	public Either<DataSet, Long> executeQuery(String alias, String sql) {
		String firstToken = sql.stripLeading().split("\\s+", 2)[0].toUpperCase();
		if (firstToken.equals("SELECT") || firstToken.equals("WITH")) {
			var result = dslContext(alias).resultQuery(sql).fetch();
			List<String> columns = Arrays.stream(result.fields()).map(Field::getName).toList();
			List<List<String>> rows = result.stream()
				.map(record -> Arrays.stream(record.intoArray())
					.map(v -> v == null ? nullValue : v.toString())
					.toList())
				.toList();
			return Either.of(new DataSet(null, columns, rows));
		} else {
			long count = dslContext(alias).query(sql).execute();
			return Either.fallback(count);
		}
	}


	private boolean rowExists(String alias, String table, List<String> columns, List<String> values) {
		List<Condition> conditions = new java.util.ArrayList<>();
		for (int i = 0; i < columns.size(); i++) {
			Field<Object> field = DSL.field(DSL.name(columns.get(i)));
			conditions.add(nullValue.equals(values.get(i)) ? field.isNull() : field.eq(values.get(i)));
		}
		return dslContext(alias)
			.selectOne()
			.from(DSL.table(DSL.name(table)))
			.where(conditions)
			.fetchOne() != null;
	}


	private String rowDescription(List<String> columns, List<String> values) {
		Map<String, String> map = new HashMap<>();
		for (int i = 0; i < columns.size(); i++) {
			map.put(columns.get(i), values.get(i));
		}
		return map.toString();
	}


}
