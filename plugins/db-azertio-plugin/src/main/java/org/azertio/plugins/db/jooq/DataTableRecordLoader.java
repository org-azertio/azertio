package org.azertio.plugins.db.jooq;

import org.azertio.core.AzertioException;
import org.azertio.core.testplan.DataTable;
import org.azertio.plugins.db.DataSet;

import java.util.List;

public class DataTableRecordLoader {

	private final int maxRows;

	public DataTableRecordLoader(int maxRows) {
		this.maxRows = maxRows;
	}

	public DataSet load(String table, DataTable dataTable) {
		var columns = dataTable.values().getFirst();
		var rows = dataTable.values().stream().skip(1).toList();
		if (rows.size() > maxRows) {
			throw new AzertioException(
				"Data table has more than {} rows, which exceeds the configured limit for assertions",
				maxRows
			);
		}
		return new DataSet(table, columns, rows);
	}
}