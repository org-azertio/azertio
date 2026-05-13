package org.azertio.plugins.db;

import org.myjtools.jexten.Extension;
import org.azertio.core.contributors.AIIndexProvider;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Extension
public class DbAIIndexProvider implements AIIndexProvider {

    @Override
    public String stepIndexJson() {
        try (var stream = getClass().getModule().getResourceAsStream("steps-index.json")) {
            if (stream == null) { return "[]"; }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "[]";
        }
    }
}