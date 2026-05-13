package org.azertio.core.contributors;

import org.myjtools.jexten.ExtensionPoint;
import org.azertio.core.AssertionFactory;
import java.util.stream.Stream;

/**
 * @author Luis Iñesta Gelabert - luiinge@gmail.com
 */
@ExtensionPoint(version = "1.0")
public interface AssertionFactoryProvider {

	Stream<AssertionFactory<?>> assertionFactories();

}
