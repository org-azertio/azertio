package org.azertio.core.contributors;

import org.myjtools.jexten.ExtensionPoint;
import org.azertio.core.persistence.Repository;

@ExtensionPoint(version = "1.0")
public interface RepositoryFactory {

	<T extends Repository> T createRepository(Class<T> type);

	default <T extends Repository> T createReadOnlyRepository(Class<T> type) {
		return createRepository(type);
	}

}