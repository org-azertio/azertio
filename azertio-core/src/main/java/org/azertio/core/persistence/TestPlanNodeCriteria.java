package org.azertio.core.persistence;

import org.azertio.core.testplan.NodeType;
import java.util.List;
import java.util.Objects;
import java.util.UUID;


/**
 * @author Luis Iñesta Gelabert - luiinge@gmail.com
 */
public sealed interface TestPlanNodeCriteria {


	record AllCriteria() implements TestPlanNodeCriteria {}

	record HasTagCriteria(String tag) implements TestPlanNodeCriteria {}

	record HasPropertyCriteria(String property, String value) implements TestPlanNodeCriteria {}

	record HasNodeTypeCriteria(NodeType nodeType) implements TestPlanNodeCriteria {}

	record HasFieldCriteria(String field, Object value) implements TestPlanNodeCriteria {}

	record HasValuedFieldCriteria(String field) implements TestPlanNodeCriteria {}

	record IsDescendantCriteria(UUID parent, int depth) implements TestPlanNodeCriteria {}

	record IsAscendantCriteria(UUID parent, int depth) implements TestPlanNodeCriteria {}

	record AndCriteria(List<TestPlanNodeCriteria> conditions) implements TestPlanNodeCriteria {}

	record OrCriteria(List<TestPlanNodeCriteria> conditions) implements TestPlanNodeCriteria {}

	record NotCriteria(TestPlanNodeCriteria condition) implements TestPlanNodeCriteria {}


	static TestPlanNodeCriteria all() {
		return new AllCriteria();
	}

	static TestPlanNodeCriteria withTag(String tag) {
		return new HasTagCriteria(Objects.requireNonNull(tag));
	}

	static TestPlanNodeCriteria withProperty(String property, String value) {
		return new HasPropertyCriteria(Objects.requireNonNull(property), value);
	}

	static TestPlanNodeCriteria withNodeType(NodeType nodeType) {
		return new HasNodeTypeCriteria(nodeType);
	}

	static TestPlanNodeCriteria withField(String field, Object value) {
		return new HasFieldCriteria(field,value);
	}

	static TestPlanNodeCriteria withField(String field) {
		return new HasValuedFieldCriteria(field);
	}

	static TestPlanNodeCriteria childOf(UUID parent) {
		return new IsDescendantCriteria(parent,1);
	}

	static TestPlanNodeCriteria descendantOf(UUID parent) {
		return new IsDescendantCriteria(parent,-1);
	}

	static TestPlanNodeCriteria descendantOf(UUID parent, int depth) {
		return new IsDescendantCriteria(parent,depth);
	}

	static TestPlanNodeCriteria parentOf(UUID child) {
		return new IsAscendantCriteria(child,1);
	}

	static TestPlanNodeCriteria ascendantOf(UUID child) {
		return new IsAscendantCriteria(child,-1);
	}

	static TestPlanNodeCriteria ascendantOf(UUID child, int depth) {
		return new IsAscendantCriteria(child,depth);
	}

	static TestPlanNodeCriteria and(TestPlanNodeCriteria... conditions) {
		return new AndCriteria(List.of(conditions));
	}

	static TestPlanNodeCriteria or(TestPlanNodeCriteria... conditions) {
		return new OrCriteria(List.of(conditions));
	}

	static TestPlanNodeCriteria not(TestPlanNodeCriteria condition) {
		return new NotCriteria(condition);
	}

}