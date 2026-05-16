package org.azertio.core;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.List;



/**
 * @author Luis Iñesta Gelabert - luiinge@gmail.com
 */
public class ResourceFinder {

	private final Path startingPath;

	public ResourceFinder(Path startingPath) {
		this.startingPath = startingPath;
	}

	public ResourceSet findResources(String globPattern) {
		return findResources(globPattern, List.of());
	}

	public ResourceSet findResources(String globPattern, List<Path> excludePaths) {
		if (!globPattern.contains("**")) {
			globPattern = "**/" + globPattern;
		}
		PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("glob:"+globPattern);
		List<Path> absExcludes = excludePaths.stream()
			.map(p -> p.toAbsolutePath().normalize())
			.toList();
		try (var stream = Files.walk(startingPath)) {
			var resources = stream.filter(Files::isRegularFile)
				.filter(path -> pathMatcher.matches(path))
				.filter(path -> {
					Path abs = path.toAbsolutePath().normalize();
					return absExcludes.stream().noneMatch(abs::startsWith);
				})
				.map(file -> new Resource(file.toUri(), file, ()->newReader(file)))
				.toList();
			return new ResourceSet(resources);
		} catch (IOException e) {
			throw new AzertioException(e,"Error reading resources from {}",startingPath);
		}
	}


	private InputStream newReader(Path path) {
		Path absolutePath = path.toAbsolutePath();
	    try {
		   return Files.newInputStream(absolutePath);
	    } catch (IOException e) {
		   throw new AzertioException(e,"Cannot read file {}",absolutePath);
	    }
	}


	public Path resolve(String file) {
		return startingPath.resolve(file);
	}

	public Path resolve(Path file) {
		return startingPath.resolve(file);
	}

	public String readAsString(String file) {
		try {
			return Files.readString(resolve(file));
		} catch (IOException e) {
			throw new AzertioException(e, "Cannot read local file {}", file);
		}
	}


}