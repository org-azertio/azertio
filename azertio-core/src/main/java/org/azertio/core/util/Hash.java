package org.azertio.core.util;

import org.azertio.core.AzertioException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;


public class Hash {

	private static final Base64.Encoder encoder = Base64.getEncoder();

	private static MessageDigest newDigest() {
		try {
			return MessageDigest.getInstance("SHA3-256");
		} catch (NoSuchAlgorithmException e) {
			throw new AzertioException(e, "Error obtaining hash algorithm");
		}
	}

	/**
	 * Creates a new Hash instance from the given string
	 * @param content the string to hash
	 * @return a new hash string
	
 * @author Luis Iñesta Gelabert - luiinge@gmail.com */
	public static String of(String content) {
		var bytes = newDigest().digest(content.getBytes(StandardCharsets.UTF_8));
		return encoder.encodeToString(bytes);
	}


	public static String of(Path path) {
		return of(List.of(path));
	}


	public static String of(Collection<? extends Path> paths) {
		List<Path> sorted = new ArrayList<>(paths);
		Collections.sort(sorted);
		MessageDigest digest = newDigest();
		for (Path path : sorted) {
			if (!path.toFile().isFile()) {
				continue;
			}
			try {
				// include file name in the hash to detect renames
				digest.update(path.toAbsolutePath().toString().getBytes(StandardCharsets.UTF_8));
				// include file content in the hash to detect content changes
				try (InputStream is = Files.newInputStream(path.toAbsolutePath())) {
					byte[] buffer = new byte[8192];
					int bytesRead;
					while ((bytesRead = is.read(buffer)) != -1) {
						digest.update(buffer, 0, bytesRead);
					}
				}
			} catch (IOException e) {
				throw new AzertioException(e,"Cannot calculate hash of {resource}",path);
			}
		}
		return encoder.encodeToString(digest.digest());
	}





}
