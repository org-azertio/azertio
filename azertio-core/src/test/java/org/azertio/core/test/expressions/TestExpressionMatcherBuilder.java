package org.azertio.core.test.expressions;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.azertio.core.AssertionFactories;
import org.azertio.core.DataTypes;
import org.azertio.core.expressions.ExpressionMatcherBuilder;
import org.azertio.core.expressions.PatternFragmentMatcher;
import java.util.stream.Stream;
import static org.assertj.core.api.Assertions.assertThat;


class TestExpressionMatcherBuilder {



	static ExpressionMatcherBuilder builder = new ExpressionMatcherBuilder(
		DataTypes.of(),
		AssertionFactories.of()
	);


	static Stream<Arguments> simpleExpressions() {
		return Stream.of(
			Arguments.of(
				"this is a simple expression",
				"(this\\s+is\\s+a\\s+simple\\s+expression)"
			),
			Arguments.of(
				"this is an \\^escaped \\(expression",
				"(this\\s+is\\s+an\\s+\\^escaped\\s+\\(expression)"
			),
			Arguments.of(
				"this is a ^negated word",
				"(this\\s+is\\s+a\\s+)(?!(negated))\\S+(\\s+word)"
			),
			Arguments.of(
				"this is a ^[negated phrase]",
				"(this\\s+is\\s+a\\s+)(?!(negated\\s+phrase)).*"
			),
			Arguments.of(
				"this is an (optional) word",
				"(this\\s+is\\s+an\\s*)(optional)?(\\s+word)"
			),
			Arguments.of(
				"this is an word (optional)",
				"(this\\s+is\\s+an\\s+word\\s*)(optional)?"
			),
			Arguments.of(
				"this is an (optional phrase)",
				"(this\\s+is\\s+an\\s*)(optional\\s+phrase)?"
			),
			Arguments.of(
				"this is a word1|word2 choice",
				"(this\\s+is\\s+a\\s+)((word1)|(word2))(\\s+choice)"
			),
			Arguments.of(
				"this is a [one phrase|another phrase] choice",
				"(this\\s+is\\s+a\\s+)((one\\s+phrase)|(another\\s+phrase))(\\s+choice)"
			),
			Arguments.of(
				"this is an optional (word1|word2) choice",
				"(this\\s+is\\s+an\\s+optional\\s*)((word1)|(word2))?(\\s+choice)"
			),
			Arguments.of(
				"this is an option(al) suffix",
				"(this\\s+is\\s+an\\s+option)(al)?(\\s+suffix)"
			),
			Arguments.of(
				"this is an optional ( one phrase| another phrase) choice",
				"(this\\s+is\\s+an\\s+optional\\s*)((\\s+one\\s+phrase)|(\\s+another\\s+phrase))?(\\s+choice)"
			),
			Arguments.of(
				"this is a wildcard: *",
				"(this\\s+is\\s+a\\s+wildcard:\\s*)(.*)"
			)
		);
	}


	@ParameterizedTest
	@MethodSource("simpleExpressions")
	void simpleExpression(String expression, String regex) {
		var fragments = builder.buildExpressionMatcher(expression).fragments();
		System.out.println(fragments);
		assertThat(fragments).hasSize(1);
		var fragment = (PatternFragmentMatcher) fragments.getFirst();
		assertThat(fragment.pattern().pattern()).isEqualTo(regex);
	}



}
