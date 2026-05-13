package org.azertio.core.testplan;
/**
 * Represents the type of a node in the test plan.
 * Each node type has a unique symbol and a numeric value.
 * The numeric value is used for sorting nodes in the plan.

 * @author Luis Iñesta Gelabert - luiinge@gmail.com */
public enum NodeType {

	/** The root node of the test plan. */
	TEST_PLAN(1),

	/** A test suite node, which can contain multiple test cases or aggregators. */
	TEST_SUITE(2),

	/** A test aggregator node, which can contain multiple test cases. */
	TEST_FEATURE(3),

	/** A test case node, which represents a single test case. */
	TEST_CASE(4),

	/** A step aggregator node, which can contain multiple steps. */
	STEP_AGGREGATOR(5),

	/** A step node, which represents a single step in a test case. */
	STEP(6),

	/** A virtual step node, which is used for grouping or organizing steps without executing them. */
	VIRTUAL_STEP(7);

	private static final NodeType[] VALUES = NodeType.values();

	public final int value;



	NodeType(int value) {
		this.value = value;
	}


	public static NodeType of(int value) {
		for (var nodeType : VALUES) {
			if (nodeType.value == value) {
				return  nodeType;
			}
		}
		throw new IllegalArgumentException();
	}
}

