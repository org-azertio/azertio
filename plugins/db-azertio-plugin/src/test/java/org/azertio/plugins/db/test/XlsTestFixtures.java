package org.azertio.plugins.db.test;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

class XlsTestFixtures {

	private static final AtomicBoolean created = new AtomicBoolean(false);

	static void create(Class<?> clazz) throws Exception {
		if (!created.compareAndSet(false, true)) return;
		createPassFixture(clazz, "xls-contains-passes");
		createFailFixture(clazz, "xls-contains-fails");
		createPassFixture(clazz, "dsl-xls-contains-passes");
		createFailFixture(clazz, "dsl-xls-contains-fails");
	}

	private static void createPassFixture(Class<?> clazz, String featureDir) throws Exception {
		createFixture(clazz, featureDir, wb -> {
			Sheet sheet = wb.createSheet("users");
			Row header = sheet.createRow(0);
			header.createCell(0).setCellValue("id");
			header.createCell(1).setCellValue("name");
			Row row1 = sheet.createRow(1);
			row1.createCell(0).setCellValue("1");
			row1.createCell(1).setCellValue("Alice");
			Row row2 = sheet.createRow(2);
			row2.createCell(0).setCellValue("2");
			row2.createCell(1).setCellValue("Bob");
		});
	}

	private static void createFailFixture(Class<?> clazz, String featureDir) throws Exception {
		createFixture(clazz, featureDir, wb -> {
			Sheet sheet = wb.createSheet("users");
			Row header = sheet.createRow(0);
			header.createCell(0).setCellValue("id");
			header.createCell(1).setCellValue("name");
			Row row = sheet.createRow(1);
			row.createCell(0).setCellValue("99");
			row.createCell(1).setCellValue("Nobody");
		});
	}

	private static void createFixture(Class<?> clazz, String featureDir, Consumer<Workbook> setup) throws Exception {
		URL dirUrl = clazz.getResource("/features/" + featureDir);
		if (dirUrl == null) return;
		Path xlsPath = Path.of(dirUrl.toURI()).resolve("data.xlsx");
		try (Workbook wb = new XSSFWorkbook();
			 FileOutputStream fos = new FileOutputStream(xlsPath.toFile())) {
			setup.accept(wb);
			wb.write(fos);
		}
	}
}