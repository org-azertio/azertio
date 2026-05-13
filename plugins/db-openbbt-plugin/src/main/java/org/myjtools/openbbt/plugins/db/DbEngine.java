package org.myjtools.openbbt.plugins.db;

import org.myjtools.openbbt.core.testplan.DataTable;
import org.myjtools.openbbt.core.util.Either;
import java.nio.file.Path;
import java.util.List;

public interface DbEngine extends AutoCloseable {


	void insertDataTable(String alias, String table, DataTable data);

	void insertCSVFile(String alias, String table, Path csvFile);

	void insertSubselect(String alias, String table, String subselect);

	void insertXLSFile(String alias, String table, Path xlsFile);

	Integer executeCountQueryFromTable(String alias, String table);

	void assertTableContains(String alias, String table, DataSet dataSet);

	void assertTableIs(String alias, String table, DataSet dataSet);

	DataSet readTable(String alias, DataTable table);

	DataSet readCsv(String table, Path file);

	List<DataSet> readXls(Path file);

	String printTable(String alias, String table);

	Either<DataSet, Long> executeQuery(String alias, String sql);

}
