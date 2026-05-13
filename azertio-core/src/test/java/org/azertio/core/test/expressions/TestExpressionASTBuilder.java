package org.azertio.core.test.expressions;

import org.junit.jupiter.api.Test;
import org.azertio.core.expressions.ExpressionASTBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.azertio.core.expressions.ExpressionASTNode.Type.*;


class
TestExpressionASTBuilder {


	@Test
	void simpleExpression() {
		var tree = new ExpressionASTBuilder("this is a simple expression").buildTree();
		System.out.println(tree);
		assertThat(tree).isEqualTo(
			LITERAL.of("this is a simple expression")
		);
	}

	@Test
	void escapedExpression() {
		var tree = new ExpressionASTBuilder("this is an \\^escaped \\(expression").buildTree();
		System.out.println(tree);
		assertThat(tree).isEqualTo(
			LITERAL.of("this is an ^escaped (expression")
		);
	}

	@Test
	void negatedWord() {
		var tree = new ExpressionASTBuilder("this is a ^negated word").buildTree();
		System.out.println(tree);
		assertThat(tree).isEqualTo(
			SEQUENCE.of(
				LITERAL.of("this is a "),
				NEGATION.of(
					LITERAL.of("negated")
				),
				LITERAL.of(" word")
			)
		);
	}


	@Test
	void negatedPhrase() {
		var tree = new ExpressionASTBuilder("this is a ^[negated phrase]").buildTree();
		System.out.println(tree);
		assertThat(tree).isEqualTo(
			SEQUENCE.of(
				LITERAL.of("this is a "),
				NEGATION.of(
					LITERAL.of("negated phrase")
				)
			)
		);
	}


	@Test
	void optionalWord() {
		var tree = new ExpressionASTBuilder("this is an (optional) word").buildTree();
		System.out.println(tree);
		assertThat(tree).isEqualTo(
			SEQUENCE.of(
				LITERAL.of("this is an "),
				OPTIONAL.of(
					LITERAL.of("optional")
				),
				LITERAL.of(" word")
			)
		);
	}



	@Test
	void optionalPhrase() {
		var tree = new ExpressionASTBuilder("this is an (optional phrase)").buildTree();
		System.out.println(tree);
		assertThat(tree).isEqualTo(
			SEQUENCE.of(
				LITERAL.of("this is an "),
				OPTIONAL.of(
					LITERAL.of("optional phrase")
				)
			)
		);
	}


	@Test
	void wordChoice() {
		var tree = new ExpressionASTBuilder("this is a word1|word2 choice").buildTree();
		System.out.println(tree);
		assertThat(tree).isEqualTo(
			SEQUENCE.of(
				LITERAL.of("this is a "),
				CHOICE.of(
					LITERAL.of("word1"),
					LITERAL.of("word2")
				),
				LITERAL.of(" choice")
			)
		);
	}



	@Test
	void phraseChoice() {
		var tree = new ExpressionASTBuilder("this is a [one phrase|another phrase] choice").buildTree();
		System.out.println(tree);
		assertThat(tree).isEqualTo(
			SEQUENCE.of(
				LITERAL.of("this is a "),
				CHOICE.of(
					LITERAL.of("one phrase"),
					LITERAL.of("another phrase")
				),
				LITERAL.of(" choice")
			)
		);
	}


	@Test
	void optionalWordChoice() {
		var tree = new ExpressionASTBuilder("this is an optional (word1 | word2) choice").buildTree();
		System.out.println(tree);
		assertThat(tree).isEqualTo(
			SEQUENCE.of(
				LITERAL.of("this is an optional "),
				OPTIONAL.of(
					CHOICE.of(
						LITERAL.of("word1 "),
						LITERAL.of(" word2")
					)
				),
				LITERAL.of(" choice")
			)
		);
	}


	@Test
	void optionalSuffix() {
		var tree = new ExpressionASTBuilder("this is an option(al) suffix").buildTree();
		System.out.println(tree);
		assertThat(tree).isEqualTo(
			SEQUENCE.of(
				LITERAL.of("this is an option"),
				OPTIONAL.of(
					LITERAL.of("al")
				),
				LITERAL.of(" suffix")
			)
		);
	}



	@Test
	void optionalPhraseChoice() {
		var tree = new ExpressionASTBuilder("""
			this is an optional ( one phrase| another phrase) choice
		""").buildTree();
		System.out.println(tree);
		assertThat(tree).isEqualTo(
			SEQUENCE.of(
				LITERAL.of("this is an optional "),
				OPTIONAL.of(
					CHOICE.of(
						LITERAL.of(" one phrase"),
						LITERAL.of(" another phrase")
					)
				),
				LITERAL.of(" choice")
			)
		);
	}


	@Test
	void wildcard() {
		var tree = new ExpressionASTBuilder("""
			this is a wildcard: *
		""").buildTree();
		System.out.println(tree);
		assertThat(tree).isEqualTo(
			SEQUENCE.of(
				LITERAL.of("this is a wildcard: "),
				WILDCARD.of()
			)
		);
	}


	@Test
	void arguments() {
		var tree = new ExpressionASTBuilder("""
			this is an unnamed argument {integer} and a named argument {name:text}
		""").buildTree();
		System.out.println(tree);
		assertThat(tree).isEqualTo(
			SEQUENCE.of(
				LITERAL.of("this is an unnamed argument "),
				ARGUMENT.of("integer"),
				LITERAL.of(" and a named argument "),
				ARGUMENT.of("name:text")
			)
		);
	}


}
