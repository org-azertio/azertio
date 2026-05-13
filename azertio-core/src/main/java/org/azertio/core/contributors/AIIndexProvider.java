package org.azertio.core.contributors;

import org.myjtools.jexten.ExtensionPoint;

@ExtensionPoint
public interface AIIndexProvider {
    String stepIndexJson();
}