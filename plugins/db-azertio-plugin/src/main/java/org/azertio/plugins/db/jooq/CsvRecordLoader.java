package org.azertio.plugins.db.jooq;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.azertio.core.AzertioException;
import org.azertio.plugins.db.DataSet;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class CsvRecordLoader {

	private static final CSVFormat CSV_FORMAT = CSVFormat.DEFAULT.builder()
		.setHeader().setSkipHeaderRecord(true).setTrim(true).build();

	private final String nullValue;
	private final int maxRows;

	public CsvRecordLoader(String nullValue, int maxRows) {
		this.nullValue = nullValue;
		this.maxRows = maxRows;
	}

	public DataSet load(String table, Path csvFile) {
		try (CSVParser parser = CSVParser.parse(csvFile, StandardCharsets.UTF_8, CSV_FORMAT)) {
			List<String> headers = new ArrayList<>(parser.getHeaderNames());
			List<List<String>> rows = new ArrayList<>();
			int count = 0;
			for (CSVRecord rec : parser) {
				count++;
				if (count > maxRows) {
					throw new AzertioException(
						"CSV file {} has more than {} rows, which exceeds the configured limit for assertions",
						csvFile,
						maxRows
					);
				}
				List<String> row = new ArrayList<>();
				for (String header : headers) {
					String value = rec.get(header);
					row.add(value == null || value.isEmpty() ? nullValue : value);
				}
				rows.add(row);
			}
			return new DataSet(table, headers, rows);
		} catch (IOException e) {
			throw new AzertioException(e, "Failed to read CSV file {}", csvFile);
		}
	}
}