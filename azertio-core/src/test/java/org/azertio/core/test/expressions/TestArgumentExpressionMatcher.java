package org.azertio.core.test.expressions;

import org.junit.jupiter.api.Test;
import org.azertio.core.AssertionFactories;
import org.azertio.core.DataTypes;
import org.azertio.core.datatypes.CoreDataTypes;
import org.azertio.core.expressions.ExpressionMatcherBuilder;
import org.azertio.core.expressions.LiteralValue;
import org.azertio.core.expressions.Match;
import org.azertio.core.expressions.VariableValue;
import java.util.Locale;
import static org.assertj.core.api.Assertions.assertThat;


class TestArgumentExpressionMatcher {


	public static final Locale LOCALE = Locale.getDefault();

	static DataTypes dataTypes = DataTypes.of(CoreDataTypes.ALL);
	static ExpressionMatcherBuilder builder = new ExpressionMatcherBuilder(dataTypes, AssertionFactories.of());


	@Test
	void testUnnamedArgumentExpression() {
		var expression = "this is a unnamed number: {integer}";
		var matcher = builder.buildExpressionMatcher(expression);
		Match match = matcher.matches("this is a unnamed number: 42", LOCALE).orElseThrow();
		assertThat(match.arguments().getFirst()).isInstanceOf(LiteralValue.class);
		assertThat(((LiteralValue) match.arguments().getFirst()).value()).isEqualTo(42);
	}

	@Test
	void testNamedInterpolateArgumentsExpression() {
		var expression = "this is a named number: {number1:integer} and another: {number2:integer}";
		var matcher = builder.buildExpressionMatcher(expression);
		Match match = matcher.matches("this is a named number: 42 and another: 84", LOCALE).orElseThrow();
		assertThat(match.arguments().get(0)).isInstanceOf(LiteralValue.class);
		assertThat(((LiteralValue) match.arguments().get(0)).value()).isEqualTo(42);
		assertThat(match.arguments().get(1)).isInstanceOf(LiteralValue.class);
		assertThat(((LiteralValue) match.arguments().get(1)).value()).isEqualTo(84);
	}


	@Test
	void testStringInterpolateArgumentsExpression() {
		var expression = "this is a text: {text1:text} and this is another: {text2:text}";
		var matcher = builder.buildExpressionMatcher(expression);
		Match match = matcher.matches("this is a text: 'hello world' and this is another: \"goodbye\"", LOCALE).orElseThrow();
		assertThat(match.arguments().get(0)).isInstanceOf(LiteralValue.class);
		assertThat(((LiteralValue) match.arguments().get(0)).value()).isEqualTo("hello world");
		assertThat(match.arguments().get(1)).isInstanceOf(LiteralValue.class);
		assertThat(((LiteralValue) match.arguments().get(1)).value()).isEqualTo("goodbye");
	}


	@Test
	void testVariableArgumentExpression() {
		var expression = "this is a unnamed number: {integer}";
		var matcher = builder.buildExpressionMatcher(expression);
		Match match = matcher.matches("this is a unnamed number: ${varnumber}", LOCALE).orElseThrow();
		assertThat(match.arguments().getFirst()).isInstanceOf(VariableValue.class);
		assertThat(((VariableValue) match.arguments().getFirst()).variable()).isEqualTo("varnumber");
		assertThat(match.arguments().getFirst().type().name()).isEqualTo("integer");
	}



}
