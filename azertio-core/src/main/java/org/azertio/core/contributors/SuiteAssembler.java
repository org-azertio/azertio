package org.azertio.core.contributors;

import org.myjtools.jexten.ExtensionPoint;
import org.azertio.core.testplan.TestSuite;
import java.util.Optional;
import java.util.UUID;


/**
 * Interface for contributors that assembles plans based on resources.
 * @author Luis Iñesta Gelabert - luiinge@gmail.com */
@ExtensionPoint(version = "1.0")
public interface SuiteAssembler extends Contributor {


	Optional<UUID> assembleSuite(TestSuite testSuite);

}
