package org.myjtools.openbbt.plugins.db;

import java.util.List;

public record DataSet(String table, List<String> columns, List<List<String>> rows) {

	public String toCsv() {
		StringBuilder sb = new StringBuilder();
		sb.append(String.join(",", columns)).append("\n");
		for (List<String> row : rows) {
			sb.append(String.join(",", row)).append("\n");
		}
		return sb.toString();
	}
}
