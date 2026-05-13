package org.azertio.core.validator;

import org.myjtools.jexten.Extension;
import org.myjtools.jexten.Inject;
import org.azertio.core.backend.StepProviderBackend;
import org.azertio.core.contributors.TestPlanValidator;
import org.azertio.core.persistence.TestPlanNodeCriteria;
import org.azertio.core.persistence.TestPlanRepository;
import org.azertio.core.testplan.*;
import org.azertio.core.util.Log;
import java.util.Locale;
import java.util.UUID;

@Extension
public class DefaultPlanValidator implements TestPlanValidator {

	private static final Log log = Log.of();

	@Inject
	TestPlanRepository repository;

	@Override
	public void validate(TestPlan plan, StepProviderBackend backend) {
		log.debug("Validating test plan: {}", plan.planID());
		validateNode(plan.planNodeRoot(), backend);
		repository.propagatePlanIssues(plan.planID());
		if (repository.planHasIssues(plan.planID())) {
			log.warn("Test plan {} has validation errors", plan.planID());
		} else {
			log.debug("Test plan {} passed validation", plan.planID());
		}
	}

	private void validateNode(UUID nodeId, StepProviderBackend backend) {
		TestPlanNode node = repository.getNodeData(nodeId).orElseThrow();
		ValidationStatus status = ValidationStatus.OK;
		String message = null;

		switch (node.nodeType()) {
			case STEP -> {
				Locale locale = localeOf(node.language());
				if (!backend.isValidStep(node.name(), locale)) {
					status = ValidationStatus.ERROR;
					message = "No matching step found for: " + node.name();
				}
			}
			case STEP_AGGREGATOR -> {
				boolean hasStepChild = repository.countNodes(TestPlanNodeCriteria.and(
					TestPlanNodeCriteria.childOf(nodeId),
					TestPlanNodeCriteria.or(
						TestPlanNodeCriteria.withNodeType(NodeType.STEP),
						TestPlanNodeCriteria.withNodeType(NodeType.VIRTUAL_STEP)
					)
				)) > 0;
				if (!hasStepChild) {
					status = ValidationStatus.ERROR;
					message = "Step aggregator has no STEP or VIRTUAL_STEP children";
				}
			}
			case TEST_CASE -> {
				boolean hasStepDescendant = repository.countNodes(TestPlanNodeCriteria.and(
					TestPlanNodeCriteria.descendantOf(nodeId),
					TestPlanNodeCriteria.withNodeType(NodeType.STEP)
				)) > 0;
				if (!hasStepDescendant) {
					status = ValidationStatus.ERROR;
					message = "Test case has no STEP descendants";
				}
			}
			default -> { /* no validation for other node types */ }
		}

		repository.setNodeValidation(nodeId, status, message);
		repository.getNodeChildren(nodeId).forEach(childId -> validateNode(childId, backend));
	}

	private Locale localeOf(String language) {
		if (language == null || language.isBlank()) return Locale.ENGLISH;
		return Locale.forLanguageTag(language);
	}
}