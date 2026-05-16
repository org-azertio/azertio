package org.azertio.core.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;

public class FileUtil {

	public static Path createSafeTempDirectory(String prefix) throws IOException {
		var permissions = PosixFilePermissions.fromString("rwx------");
		var fileAttribute = PosixFilePermissions.asFileAttribute(permissions);
		return Files.createTempDirectory(prefix, fileAttribute);
	}

	public static Path createSafeTempFile(String prefix, String suffix) throws IOException {
		Path secureTempDir = createSafeTempDirectory("azertio-temp-files");
		return Files.createTempFile(secureTempDir, prefix, suffix);
	}


}
