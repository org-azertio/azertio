package org.azertio.core.docgen;

import java.util.List;
import java.util.Map;

public record StepDocEntry(
    String since,
    String role,
    String description,
    List<ParameterDoc> parameters,
    String additionalData,
    Map<String, StepLanguageEntry> language
) {
}