package org.azertio.core.contributors;

import org.myjtools.imconfig.Config;
import org.myjtools.jexten.ExtensionPoint;

@ExtensionPoint(version = "1.0")
public interface StepProvider {

	void init(Config config);

}
