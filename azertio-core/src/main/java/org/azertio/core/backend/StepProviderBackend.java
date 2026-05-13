package org.azertio.core.backend;

import org.myjtools.imconfig.Config;
import org.azertio.core.*;
import org.azertio.core.contributors.AssertionFactoryProvider;
import org.azertio.core.contributors.DataTypeProvider;
import org.azertio.core.contributors.StepProvider;
import org.azertio.core.execution.NoMatchingStepException;
import org.azertio.core.expressions.Match;
import org.azertio.core.messages.MessageProvider;
import org.azertio.core.messages.Messages;
import org.azertio.core.testplan.NodeArgument;
import org.azertio.core.util.Pair;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class StepProviderBackend {

	private final List<StepProviderService> services = new ArrayList<>();
	private final ConcurrentHashMap<String,Object> variables = new ConcurrentHashMap<>();
	private final StepProviderHinter hinter;
	private final Config config;
	private final AzertioRuntime runtime;

	public StepProviderBackend(AzertioRuntime runtime) {
		this.runtime = runtime;
		var dataTypes = DataTypes.of(runtime.getExtensions(DataTypeProvider.class)
			.flatMap(DataTypeProvider::dataTypes)
			.toList());
		var assertionFactories = AssertionFactories.of(runtime.getExtensions(AssertionFactoryProvider.class)
			.flatMap(AssertionFactoryProvider::assertionFactories)
			.toList());
		var stepProviders = runtime.getExtensions(StepProvider.class).toList();
		for (var stepProvider : stepProviders) {
			Messages messages = Messages.of(runtime.getExtensions(MessageProvider.class)
				.filter(mp -> mp.providerFor(stepProvider.getClass().getSimpleName()))
				.toList()
			);
			services.add(new StepProviderService(stepProvider, dataTypes, assertionFactories, messages));
		}
		this.hinter = new StepProviderHinter(services);
		this.config = runtime.configuration();
	}



	public void setUp(UUID executionID, UUID executionNodeID, Map<String,String> properties) {
		ExecutionContext.setCurrent(new ExecutionContext(runtime, executionID, executionNodeID));
		for (var service : services) {
			service.setUp(config.append(Config.ofMap(properties)));
		}
	}

	public void tearDown() {
		for (var service : services) {
			service.tearDown();
		}
		ExecutionContext.clearCurrent();
	}

	public Optional<Pair<StepProviderMethod, Match>> matchingStep(String step, Locale locale) {
		for (var service : services) {
			var match = service.matchingStep(step, locale);
			if (match.isPresent()) {
				return match;
			}
		}
		return Optional.empty();
	}

	public boolean isValidStep(String step, Locale locale) {
		return matchingStep(step, locale).isPresent();
	}

	public List<String> allStepsForLocale(Locale locale) {
		return services.stream()
			.flatMap(s -> s.stepStringsForLocale(locale).stream())
			.toList();
	}

	public List<Map.Entry<String, String>> allStepsWithLabelForLocale(Locale locale) {
		return services.stream()
			.flatMap(s -> s.stepStringsForLocale(locale).stream()
				.map(step -> Map.entry(s.providerLabel(), step)))
			.toList();
	}

	public List<String> hintsForStep(String invalidStep, Locale locale, int limit) {
		return hinter.getHintsForInvalidStep(invalidStep, locale, limit);
	}



	public void run(String step, Locale locale, NodeArgument nodeArgument, UUID executionNodeID) {
		var ctx = ExecutionContext.current();
		if (ctx != null && executionNodeID != null) {
			ctx.setExecutionNodeID(executionNodeID);
		}
		var matchingStep = matchingStep(step,locale).orElseThrow(
			() -> new NoMatchingStepException(
				"No matching step found for '{}'\n{}",
				step,
				hints(step,locale)
			)
		);
		var stepMethod = matchingStep.left();
		var match = matchingStep.right();
		Map<String,Object> arguments = match.interpolateArguments(variables);
		Assertion assertion = match.assertion();
		try {
			// expected either an assertion or a node argument, but not both.
			// If both are provided, the node argument takes precedence over the assertion
			stepMethod.run(arguments, nodeArgument != null ? nodeArgument : assertion);
		} catch (AssertionError | AzertioException e) {
			// AssertionErrors are not wrapped in AzertioException, as they are expected to be thrown by
			// the step methods when an assertion fails
			throw e;
		}  catch (Throwable e) {
			throw new AzertioException(e);
		}
	}



	private String hints(String invalidStep, Locale locale) {
		int maxSuggestions = 5;
		StringBuilder hint = new StringBuilder(
			"Perhaps you mean one of the following:\n\t----------\n\t"
		);
		var allSuggestions = hinter.getHintsForInvalidStep(invalidStep, locale,  -1);
		if (allSuggestions.size() > maxSuggestions) {
			allSuggestions = hinter.getHintsForInvalidStep(invalidStep, locale,  maxSuggestions);
		}
		for (String stepHint : allSuggestions) {
			hint.append(stepHint).append("\n\t");
		}
		return hint.toString();
	}



}
