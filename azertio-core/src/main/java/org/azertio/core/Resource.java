package org.azertio.core;

import org.azertio.core.util.Hash;
import org.azertio.core.util.Lazy;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.function.Supplier;


/**
 * @author Luis Iñesta Gelabert - luiinge@gmail.com
 */
public class Resource implements Comparable<Resource> {

	private final URI URI;
	private final Path relativePath;
	private final Supplier<InputStream> reader;
	private final Lazy<String> hash;


	public Resource(URI URI, Path relativePath, Supplier<InputStream> reader) {
		this.URI = URI;
		this.relativePath = relativePath;
		this.reader = reader;
		this.hash = Lazy.of(() -> Hash.of(relativePath));
	}


	public InputStream open() {
		return reader.get();
	}

	public String hash() {
		return hash.get();
	}

	public URI URI() {
		return URI;
	}

	public Path relativePath() {
		return relativePath;
	}

	public String extension() {
		String name = relativePath.getFileName().toString();
		int dotIndex = name.lastIndexOf('.');
		return (dotIndex != -1) ? name.substring(dotIndex + 1) : "";
	}

	@Override
	public int compareTo(Resource other) {
		return this.relativePath.compareTo(other.relativePath);
	}

	@Override
	public int hashCode() {
		return this.relativePath.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null || getClass() != obj.getClass()) return false;
		Resource other = (Resource) obj;
		return this.relativePath.equals(other.relativePath);
	}

}
