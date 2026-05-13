package org.azertio.core.contributors;

import org.myjtools.jexten.ExtensionPoint;
import java.util.UUID;

@ExtensionPoint(version = "1.0")
public interface ReportBuilder {

	void buildReport(UUID executionID);

}
