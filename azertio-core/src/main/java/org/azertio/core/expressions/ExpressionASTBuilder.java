package org.azertio.core.expressions;

import java.util.*;


/**
 * Parser that builds an Abstract Syntax Tree (AST) from expression tokens.
 *
 * <p>The builder uses a state machine to parse the token stream and construct
 * an {@link ExpressionASTNode} tree representing the expression structure.</p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * ExpressionASTBuilder builder = new ExpressionASTBuilder("click (the) button");
 * ExpressionASTNode tree = builder.buildTree();
 *
 * // Tree structure:
 * // SEQUENCE [
 * //   LITERAL<click >
 * //   OPTIONAL [
 * //     LITERAL<the>
 * //   ]
 * //   LITERAL< button>
 * // ]
 * }</pre>
 *
 * <h2>Supported Constructs</h2>
 * <ul>
 *   <li>Literal text</li>
 *   <li>Optional groups: {@code (optional text)}</li>
 *   <li>Required groups: {@code [required text]}</li>
 *   <li>Choices: {@code word1|word2} or {@code (phrase1|phrase2)}</li>
 *   <li>Negation: {@code ^word} or {@code ^[phrase]}</li>
 *   <li>Wildcards: {@code *}</li>
 *   <li>Arguments: {@code {type}} or {@code {name:type}}</li>
 *   <li>Assertions: {@code {{assertion-name}}}</li>
 * </ul>
 *
 * @author Luis Iñesta Gelabert - luiinge@gmail.com
 * @see ExpressionASTNode
 * @see ExpressionTokenizer
 */
public class ExpressionASTBuilder {

	private enum State {
		DEFAULT,
		NEGATION,
		NEGATION_GROUP,
		OPTIONAL,
		OPTIONAL_CHOICE,
		WORD_CHOICE,
		GROUP,
		GROUP_CHOICE,
		ARGUMENT,
		ASSERTION
	}

	private final Iterator<ExpressionToken> tokens;
	private final Deque<State> stateStack = new LinkedList<>();
	private final Deque<ExpressionASTNode> nodeStack = new LinkedList<>();

	private ExpressionToken currentToken;
	private ExpressionToken previousToken;




	ExpressionASTBuilder(Iterator<ExpressionToken> tokens) {
		this.tokens = tokens;
	}


	public ExpressionASTBuilder(String text) {
		this(new ExpressionTokenizer(text).tokens().iterator());
	}




	public ExpressionASTNode buildTree() {

		pushState(State.DEFAULT, ExpressionASTNode.Type.SEQUENCE.empty());

		while (tokens.hasNext()) {
			previousToken = currentToken;
			currentToken = tokens.next();
			processToken();

		}

		if (stateStack.getLast() != State.DEFAULT) {
			throw new ExpressionException("unexpected final state {}",stateStack.getLast());
		}
		return nodeStack.getLast().reduced();

	}


	private void processToken() {
		switch (stateStack.getLast()) {
			case DEFAULT -> processDefaultState();
			case NEGATION -> processNegationState();
			case NEGATION_GROUP -> processNegationGroupState();
			case OPTIONAL -> processOptionalState();
			case OPTIONAL_CHOICE -> processOptionalChoiceState();
			case GROUP -> processGroupState();
			case GROUP_CHOICE -> processGroupChoiceState();
			case WORD_CHOICE -> processWordChoiceState();
			case ARGUMENT -> processArgumentState();
			case ASSERTION -> processAssertionState();
			default -> abort(currentToken);
		}
	}


	private void processDefaultState() {
		var parentNode = nodeStack.getLast().assertType(ExpressionASTNode.Type.SEQUENCE);
		switch (currentToken.type()) {
			case TEXT -> parentNode.add(ExpressionASTNode.Type.LITERAL.of(currentToken));
			case WILDCARD -> parentNode.add(ExpressionASTNode.Type.WILDCARD.empty());
			case NEGATION -> pushState(State.NEGATION, ExpressionASTNode.Type.NEGATION);
			case START_OPTIONAL -> pushState(State.OPTIONAL, ExpressionASTNode.Type.OPTIONAL);
			case START_GROUP -> pushState(State.GROUP, ExpressionASTNode.Type.SEQUENCE);
			case START_ARGUMENT -> pushState(State.ARGUMENT, ExpressionASTNode.Type.ARGUMENT);
			case START_ASSERTION -> pushState(State.ASSERTION, ExpressionASTNode.Type.ASSERTION);
			case CHOICE_SEPARATOR -> processDefaultChoiceSeparator();
			default -> abort(currentToken);
		}
	}


	private void processDefaultChoiceSeparator() {
		var parentNode = nodeStack.getLast().assertType(ExpressionASTNode.Type.SEQUENCE);
		assertToken(previousToken, ExpressionTokenType.TEXT);
		if (previousToken.endsWithBlank()) {
			abort(currentToken);
		}
		ExpressionASTNode lastNode = parentNode.lastChild();
		parentNode.remove(lastNode);
		if (previousToken.isSingleWord()) {
			pushState(State.WORD_CHOICE, ExpressionASTNode.Type.CHOICE.of(lastNode));
		} else {
			String lastWord = previousToken.lastWord();
			parentNode.add(
				ExpressionASTNode.Type.LITERAL.of(previousToken.removeTrailingChars(lastWord.length()))
			);
			pushState(State.WORD_CHOICE, ExpressionASTNode.Type.CHOICE.of(ExpressionASTNode.Type.LITERAL.of(lastWord)));
		}
	}


	private void processNegationState() {
		var parentNode = nodeStack.getLast().assertType(ExpressionASTNode.Type.NEGATION);
		switch (currentToken.type()) {
			case TEXT -> {
				if (currentToken.startsWithBlank()) {
					abort(currentToken);
				}
				if (currentToken.isSingleWord()) {
					parentNode.add(ExpressionASTNode.Type.LITERAL.of(currentToken));
					popState();
				} else {
					String firstWord = currentToken.firstWord();
					parentNode.add(ExpressionASTNode.Type.LITERAL.of(firstWord));
					popState();
					previousToken = currentToken.firstWordToken();
					currentToken = currentToken.removeLeadingChars(firstWord.length());
					processToken();
				}
			}
			case START_GROUP -> mutateState(State.NEGATION_GROUP);
			default -> abort(currentToken);
		}
	}


	private void processNegationGroupState() {
		var parentNode = nodeStack.getLast().assertType(ExpressionASTNode.Type.NEGATION);
		switch (currentToken.type()) {
			case TEXT -> { }
			case END_GROUP -> {
				assertToken(previousToken, ExpressionTokenType.TEXT);
				parentNode.add(ExpressionASTNode.Type.LITERAL.of(previousToken));
				popState();
			}
			default -> abort(currentToken);
		}
	}


	private void processOptionalState() {
		var parentNode = nodeStack.getLast().assertType(ExpressionASTNode.Type.OPTIONAL);
		switch (currentToken.type()) {
			case TEXT -> { }
			case END_OPTIONAL -> {
				assertToken(previousToken, ExpressionTokenType.TEXT);
				parentNode.add(ExpressionASTNode.Type.LITERAL.of(previousToken));
				popState();
			}
			case CHOICE_SEPARATOR -> {
				assertToken(previousToken, ExpressionTokenType.TEXT);
				pushState(State.OPTIONAL_CHOICE, ExpressionASTNode.Type.CHOICE);
				ExpressionASTNode choiceNode = nodeStack.getLast();
				choiceNode.add(ExpressionASTNode.Type.LITERAL.of(previousToken));
			}
			default -> abort(currentToken);
		}
	}


	private void processOptionalChoiceState() {
		var parentNode = nodeStack.getLast().assertType(ExpressionASTNode.Type.CHOICE);
		switch (currentToken.type()) {
			case TEXT -> { }
			case CHOICE_SEPARATOR -> {
				assertToken(previousToken, ExpressionTokenType.TEXT);
				parentNode.add(ExpressionASTNode.Type.LITERAL.of(previousToken));
			}
			case END_OPTIONAL -> {
				assertToken(previousToken, ExpressionTokenType.TEXT);
				parentNode.add(ExpressionASTNode.Type.LITERAL.of(previousToken));
				popState();
				popState();
			}
			default -> abort(currentToken);
		}
	}


	private void processGroupState() {
		var parentNode = nodeStack.getLast().assertType(ExpressionASTNode.Type.SEQUENCE);
		switch (currentToken.type()) {
			case TEXT -> { }
			case END_GROUP -> {
				assertToken(previousToken, ExpressionTokenType.TEXT);
				parentNode.add(ExpressionASTNode.Type.LITERAL.of(previousToken));
				popState();
			}
			case CHOICE_SEPARATOR -> {
				assertToken(previousToken, ExpressionTokenType.TEXT);
				pushState(State.GROUP_CHOICE, ExpressionASTNode.Type.CHOICE);
				ExpressionASTNode choiceNode = nodeStack.getLast();
				choiceNode.add(ExpressionASTNode.Type.LITERAL.of(previousToken));
			}
			default -> abort(currentToken);
		}
	}


	private void processGroupChoiceState() {
		var parentNode = nodeStack.getLast().assertType(ExpressionASTNode.Type.CHOICE);
		switch (currentToken.type()) {
			case TEXT -> { }
			case CHOICE_SEPARATOR -> {
				assertToken(previousToken, ExpressionTokenType.TEXT);
				parentNode.add(ExpressionASTNode.Type.LITERAL.of(previousToken));
			}
			case END_GROUP -> {
				assertToken(previousToken, ExpressionTokenType.TEXT);
				parentNode.add(ExpressionASTNode.Type.LITERAL.of(previousToken));
				popState();
				popState();
			}
			default -> abort(currentToken);
		}
	}


	private void processWordChoiceState() {
		var parentNode = nodeStack.getLast().assertType(ExpressionASTNode.Type.CHOICE);
		switch (currentToken.type()) {
			case TEXT -> {
				if (currentToken.startsWithBlank()) {
					abort(currentToken);
				}
				if (!currentToken.isSingleWord()) {
					String firstWord = currentToken.firstWord();
					parentNode.add(ExpressionASTNode.Type.LITERAL.of(firstWord));
					popState();
					previousToken = currentToken.firstWordToken();
					currentToken = currentToken.removeLeadingChars(firstWord.length());
					processToken();
				}
			}
			case CHOICE_SEPARATOR -> {
				assertToken(previousToken, ExpressionTokenType.TEXT);
				parentNode.add(ExpressionASTNode.Type.LITERAL.of(previousToken));
			}
			default -> abort(currentToken);
		}
	}



	private void processArgumentState() {
		var parentNode = nodeStack.getLast().assertType(ExpressionASTNode.Type.ARGUMENT);
		switch (currentToken.type()) {
			case TEXT -> {
				if (!currentToken.isSingleWord()) {
					abort(currentToken);
				}
			}
			case END_ARGUMENT -> {
				assertToken(previousToken, ExpressionTokenType.TEXT);
				parentNode.value = previousToken.value();
				popState();
			}
			default -> abort(currentToken);
		}
	}


	private void processAssertionState() {
		var parentNode = nodeStack.getLast().assertType(ExpressionASTNode.Type.ASSERTION);
		switch (currentToken.type()) {
			case TEXT -> {
				if (!currentToken.isSingleWord()) {
					abort(currentToken);
				}
			}
			case END_ASSERTION -> {
				assertToken(previousToken, ExpressionTokenType.TEXT);
				parentNode.value = previousToken.value();
				popState();
			}
			default -> abort(currentToken);
		}
	}

	private void assertToken(ExpressionToken token, ExpressionTokenType type) {
		if (token == null || token.type() != type) {
			abort(currentToken);
		}
	}


	private void pushState(State newState, ExpressionASTNode.Type newNodeType) {
		pushState(newState, newNodeType.empty());
	}


	private void pushState(State newState, ExpressionASTNode newNode) {
		this.stateStack.addLast(newState);
		this.nodeStack.addLast(newNode);
	}


	private void mutateState(State newState) {
		this.stateStack.removeLast();
		this.stateStack.addLast(newState);
	}


	private void popState() {
		this.stateStack.removeLast();
		var currentNode = this.nodeStack.removeLast();
		this.nodeStack.getLast().add(currentNode);
	}


	private void abort(ExpressionToken currentToken) {
		throw new ExpressionException("Unexpected token {}", currentToken);
	}
	
}
