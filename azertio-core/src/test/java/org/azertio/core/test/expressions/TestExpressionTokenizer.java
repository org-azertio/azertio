package org.azertio.core.test.expressions;

import org.junit.jupiter.api.Test;
import org.azertio.core.expressions.ExpressionToken;
import org.azertio.core.expressions.ExpressionTokenType;
import org.azertio.core.expressions.ExpressionTokenizer;
import static org.assertj.core.api.Assertions.assertThat;


class TestExpressionTokenizer {


	@Test
	void tokenize() {
		var tokens = new ExpressionTokenizer("""
		   This is a simple expression, with some symbols (like these), \
		   or [these]. ^Negated (words|phrases), {{subexpressions}} and also \
		   arguments like {number}. Escaped symbols are: \\^\\(\\)\\[\\]\\{\\}\\|\\\\.
		   """).tokens();
		System.out.println(tokens);
		assertThat(tokens).containsExactly(
			new ExpressionToken(ExpressionTokenType.TEXT, "This is a simple expression, with some symbols ", 0, 47),
			new ExpressionToken(ExpressionTokenType.START_OPTIONAL,  48),
			new ExpressionToken(ExpressionTokenType.TEXT, "like these", 49, 58),
			new ExpressionToken(ExpressionTokenType.END_OPTIONAL,  59),
			new ExpressionToken(ExpressionTokenType.TEXT, ", or ", 60, 64),
			new ExpressionToken(ExpressionTokenType.START_GROUP,  65),
			new ExpressionToken(ExpressionTokenType.TEXT, "these", 66, 70),
			new ExpressionToken(ExpressionTokenType.END_GROUP,  71, 71),
			new ExpressionToken(ExpressionTokenType.TEXT, ". ", 72, 73),
			new ExpressionToken(ExpressionTokenType.NEGATION,  74),
			new ExpressionToken(ExpressionTokenType.TEXT, "Negated ", 75, 82),
			new ExpressionToken(ExpressionTokenType.START_OPTIONAL,  83, 83),
			new ExpressionToken(ExpressionTokenType.TEXT, "words", 84, 88),
			new ExpressionToken(ExpressionTokenType.CHOICE_SEPARATOR,  89),
			new ExpressionToken(ExpressionTokenType.TEXT, "phrases", 90, 96),
			new ExpressionToken(ExpressionTokenType.END_OPTIONAL,  97),
			new ExpressionToken(ExpressionTokenType.TEXT, ", ", 98, 99),
			new ExpressionToken(ExpressionTokenType.START_ASSERTION,  100),
			new ExpressionToken(ExpressionTokenType.TEXT, "subexpressions", 101, 115),
			new ExpressionToken(ExpressionTokenType.END_ASSERTION,  116),
			new ExpressionToken(ExpressionTokenType.TEXT, " and also arguments like ", 117, 142),
			new ExpressionToken(ExpressionTokenType.START_ARGUMENT,  143),
			new ExpressionToken(ExpressionTokenType.TEXT, "number", 144, 149),
			new ExpressionToken(ExpressionTokenType.END_ARGUMENT,  150),
			new ExpressionToken(ExpressionTokenType.TEXT, ". Escaped symbols are: ^()[]{}|\\.", 151, 192)
		);
	}


   

}
