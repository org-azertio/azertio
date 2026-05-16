package org.azertio.core.util;

import org.azertio.core.Clock;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.ZonedDateTime;

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

	/**
	 * Resolves a filename pattern replacing date/time tokens with values from the given clock.
	 * Supported tokens: %Y (year), %m (month), %d (day), %h (hour), %M (minute), %s (second).
	 */
	public static String resolvePattern(String pattern, Clock clock) {
		ZonedDateTime dt = clock.now().atZone(clock.zone());
		return pattern
			.replace("%Y", String.format("%04d", dt.getYear()))
			.replace("%m", String.format("%02d", dt.getMonthValue()))
			.replace("%d", String.format("%02d", dt.getDayOfMonth()))
			.replace("%h", String.format("%02d", dt.getHour()))
			.replace("%M", String.format("%02d", dt.getMinute()))
			.replace("%s", String.format("%02d", dt.getSecond()));
	}


}
