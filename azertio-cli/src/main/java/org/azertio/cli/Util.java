package org.azertio.cli;

import org.azertio.core.AzertioException;
import org.azertio.core.util.Log;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

public class Util {

	private static Log log = Log.of();

	static void deleteDirectory(Path pluginsPath) {
		if (pluginsPath.toFile().exists()) {
			try {
				Files.walk(pluginsPath)
						.sorted(Comparator.reverseOrder())
						.forEach(p -> {
							try {
								Files.delete(p);
							} catch (IOException e) {
								throw new AzertioException("No se pudo borrar: " + p, e);
							}
						});
			} catch (Exception e) {
				log.error(e,"Failed to clean existing plugins: {}", e.getMessage());
			}
		}
	}

}
