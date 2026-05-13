package org.myjtools.openbbt.core.contributors;

import org.myjtools.jexten.ExtensionPoint;

@ExtensionPoint
public interface AIIndexProvider {
    String stepIndexJson();
}