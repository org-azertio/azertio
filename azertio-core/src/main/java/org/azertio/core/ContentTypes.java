package org.azertio.core;

import org.azertio.core.contributors.ContentType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class ContentTypes {

	private final List<ContentType> contentTypes;

	private ContentTypes(List<ContentType> contentTypes) {
		this.contentTypes = contentTypes;
	}

	public static ContentTypes of(Collection<ContentType> contentTypes) {
		return new ContentTypes(List.copyOf(contentTypes));
	}

	public Optional<ContentType> get(String contentType) {
		return contentTypes.stream()
				.filter(comparator -> comparator.accepts(contentType))
				.findFirst();
	}

}
