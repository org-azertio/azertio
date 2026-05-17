package org.azertio.plugins.db.jooq;

import org.apache.poi.ss.usermodel.*;
import org.azertio.core.util.FileUtil;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class ExcelUtils {

	public static Path sheetToCSV(Sheet sheet) throws IOException {
		Path tempFile = FileUtil.createSafeTempFile("excel_sheet_", ".csv");
		try (BufferedWriter writer = Files.newBufferedWriter(tempFile)) {
			for (Row row : sheet) {
				for (int i = 0; i < row.getLastCellNum(); i++) {
					Cell cell = row.getCell(i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
					String value = cell.toString().replace("\"", "\"\"");
					writer.write("\"" + value + "\"");
					if (i < row.getLastCellNum() - 1) writer.write(",");
				}
				writer.newLine();
			}
		}
		tempFile.toFile().deleteOnExit();
		return tempFile;
	}
}