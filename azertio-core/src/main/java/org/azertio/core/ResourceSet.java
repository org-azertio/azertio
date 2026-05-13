package org.azertio.core;

import org.azertio.core.util.Hash;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class ResourceSet implements Iterable<Resource>{

	private final List<Resource> resources;
	private final String hash;

	public ResourceSet(Collection<Resource> resources) {
		this.resources = resources.stream().sorted().distinct().toList();
		this.hash = Hash.of(resources.stream().map(Resource::relativePath).toList());
	}

	public List<Resource> resources() {
		return resources;
	}

	public Stream<Resource> filter(Predicate<Resource> predicate) {
		return resources.stream().filter(predicate);
	}

	public Stream<Resource> filter(String globPattern) {
		PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("glob:"+globPattern);
		return resources.stream().filter(resource -> pathMatcher.matches(resource.relativePath()));
	}

	public int size() {
		return resources.size();
	}

	public Resource get(int index) {
		return resources.get(index);
	}

	public boolean isEmpty() {
		return resources.isEmpty();
	}

	public String hash() {
		return hash;
	}

	@Override
	public Iterator<Resource> iterator() {
		return resources.iterator();
	}

	@Override
	public void forEach(Consumer<? super Resource> action) {
		resources.forEach(action);
	}

	@Override
	public Spliterator<Resource> spliterator() {
		return resources.spliterator();
	}

}
