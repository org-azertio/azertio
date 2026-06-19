package org.azertio.cli.test;

import org.myjtools.jexten.Extension;
import org.azertio.core.contributors.HelpProvider;

@Extension
public class TestHelpProvider implements HelpProvider {

    @Override
    public String id() {
        return "test-topic";
    }

    @Override
    public String displayName() {
        return "Test Topic";
    }

    @Override
    public String help() {
        return "This is the test topic content.";
    }
}