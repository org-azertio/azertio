package org.azertio.core.contributors;

import org.myjtools.jexten.ExtensionPoint;

@ExtensionPoint
public interface HelpProvider extends Contributor {

    String id();

    String displayName();

    String help();

}