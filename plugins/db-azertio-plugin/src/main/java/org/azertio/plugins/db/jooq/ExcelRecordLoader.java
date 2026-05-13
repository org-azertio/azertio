package org.azertio.plugins.db.jooq;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jooq.*;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.azertio.core.AzertioException;
import org.azertio.plugins.db.DataSet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class ExcelRecordLoader {

	private final DSLContext ctx;
	private final String nullValue;

	public ExcelRecordLoader(DSLContext ctx, String nullValue) {
		this.ctx = ctx;
		this.nullValue = nullValue;
	}

	public ExcelRecordLoader(String nullValue) {
		this.ctx = null;
		this.nullValue = nullValue;
	}


	public void loadExcel(String filePath) throws IOException {

		File excelFile = new File(filePath);

		try (Workbook workbook = new XSSFWorkbook(new FileInputStream(excelFile))) {

			int numberOfSheets = workbook.getNumberOfSheets();
			SQLDialect dialect = ctx.dialect();

			for (int i = 0; i < numberOfSheets; i++) {

				Sheet sheet = workbook.getSheetAt(i);
				String tableName = sheet.getSheetName();

				if (supportsNativeLoad(dialect)) {
					Path csvTemp = ExcelUtils.sheetToCSV(sheet);
					nativeLoad(csvTemp, tableName, dialect);
				} else {
					genericLoad(sheet, tableName);
				}
			}
		}
	}

	private boolean supportsNativeLoad(SQLDialect dialect) {
		return dialect.family() == SQLDialect.POSTGRES ||
				dialect.family() == SQLDialect.MYSQL;
	}

	/** Fallback genérico usando loadRecords(stream) */
	private void genericLoad(Sheet sheet, String tableName) {
		Iterator<Row> rowIterator = sheet.iterator();
		if (!rowIterator.hasNext()) return;

		Row headerRow = rowIterator.next();
		List<Field<Object>> fields = new ArrayList<>();
		for (Cell cell : headerRow) {
			fields.add(DSL.field(DSL.name(cell.getStringCellValue())));
		}

		Table<?> table = DSL.table(DSL.name(tableName));
		Iterable<Row> iterable = () -> rowIterator;

		Stream<Record> recordStream = StreamSupport.stream(iterable.spliterator(), false)
				.map(row -> {
					Record rec = ctx.newRecord(table);
					for (int j = 0; j < fields.size(); j++) {
						Cell cell = row.getCell(j, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
						rec.set(fields.get(j), getCellValue(cell));
					}
					return rec;
				});

		try {
			ctx.loadInto(table)
					.loadRecords(recordStream)
					.fields(fields)
					.execute();
		} catch (IOException e) {
			throw new AzertioException(e, "Failed to load records into table {}", tableName);
		}
	}

	/** Implementación simple de carga nativa para PostgreSQL/MySQL */
	private void nativeLoad(Path csvPath, String tableName, SQLDialect dialect) {
		if (dialect.family() == SQLDialect.POSTGRES) {
			// PostgreSQL COPY
			ctx.connection(conn -> {
				try (java.sql.Statement stmt = conn.createStatement()) {
					stmt.execute("COPY " + tableName +
							" FROM '" + csvPath.toAbsolutePath() + "' " +
							"WITH CSV HEADER");
				}
			});
		} else if (dialect.family() == SQLDialect.MYSQL) {
			ctx.connection(conn -> {
				try (java.sql.Statement stmt = conn.createStatement()) {
					stmt.execute("LOAD DATA LOCAL INFILE '" + csvPath.toAbsolutePath() +
							"' INTO TABLE " + tableName +
							" FIELDS TERMINATED BY ',' ENCLOSED BY '\"' " +
							"LINES TERMINATED BY '\\n' IGNORE 1 LINES");
				}
			});
		}
	}

	public List<DataSet> readExcel(Path file, int maxRows) throws IOException {
		List<DataSet> result = new ArrayList<>();
		try (Workbook workbook = new XSSFWorkbook(new FileInputStream(file.toFile()))) {
			for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
				Sheet sheet = workbook.getSheetAt(i);
				result.add(readSheet(sheet, sheet.getSheetName(), maxRows));
			}
		}
		return result;
	}

	private DataSet readSheet(Sheet sheet, String tableName, int maxRows) {
		Iterator<Row> rowIterator = sheet.iterator();
		if (!rowIterator.hasNext()) {
			return new DataSet(tableName, List.of(), List.of());
		}
		Row headerRow = rowIterator.next();
		List<String> headers = new ArrayList<>();
		for (Cell cell : headerRow) {
			headers.add(cell.getStringCellValue());
		}
		List<List<String>> rows = new ArrayList<>();
		int count = 0;
		while (rowIterator.hasNext()) {
			count++;
			if (count > maxRows) {
				throw new AzertioException(
					"Excel sheet {} has more than {} rows, which exceeds the configured limit for assertions",
					tableName, maxRows
				);
			}
			Row row = rowIterator.next();
			List<String> rowValues = new ArrayList<>();
			for (int j = 0; j < headers.size(); j++) {
				Cell cell = row.getCell(j, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
				Object value = getCellValue(cell);
				rowValues.add(value == null ? nullValue : value.toString());
			}
			rows.add(rowValues);
		}
		return new DataSet(tableName, headers, rows);
	}

	private Object getCellValue(Cell cell) {
		switch (cell.getCellType()) {
			case STRING:
				String value = cell.getStringCellValue();
				if (value.equalsIgnoreCase(nullValue)) {
					return null;
				}
				return value;
			case NUMERIC:
				if (DateUtil.isCellDateFormatted(cell)) return cell.getDateCellValue();
				double d = cell.getNumericCellValue();
				return d == (long) d ? (long)d : d;
			case BOOLEAN: return cell.getBooleanCellValue();
			case BLANK: return "";
			default: return cell.toString();
		}
	}
}