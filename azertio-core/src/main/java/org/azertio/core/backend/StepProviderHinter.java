package org.azertio.core.backend;

import org.azertio.core.util.StringDistance;
import java.util.List;
import java.util.Locale;

public class StepProviderHinter {

	private final List<StepProviderService> services;

	public StepProviderHinter(List<StepProviderService> services) {
		this.services = services;
	}

	public List<String> getHintsForInvalidStep(
		String invalidStep,
		Locale stepLocale,
		int limit
	) {
		var candidates = services.stream()
			.flatMap(s -> s.stepStringsForLocale(stepLocale).stream())
			.toList();
		return StringDistance.closerStrings(invalidStep, candidates, limit);
	}
}