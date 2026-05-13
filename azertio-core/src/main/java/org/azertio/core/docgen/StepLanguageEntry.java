package org.azertio.core.docgen;

import java.util.List;

public record StepLanguageEntry(
    String expression,
    String example,
    List<ScenarioExample> scenarios,
    List<String> assertionHints
) {
}