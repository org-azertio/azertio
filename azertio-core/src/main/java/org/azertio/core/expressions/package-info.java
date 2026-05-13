/**
 * Provides the expression parsing and matching engine for Azertio.
 *
 * <p>This package implements a flexible expression language for defining BDD-style
 * step patterns. The expression engine supports natural language patterns with
 * optional parts, choices, negations, wildcards, typed arguments, and assertions.</p>
 *
 * <h2>Expression Processing Pipeline</h2>
 *
 * <p>Expressions are processed in three stages:</p>
 * <ol>
 *   <li><strong>Tokenization</strong> - {@link org.azertio.core.expressions.ExpressionTokenizer}
 *       converts the expression string into a list of {@link org.azertio.core.expressions.ExpressionToken}s</li>
 *   <li><strong>AST Building</strong> - {@link org.azertio.core.expressions.ExpressionASTBuilder}
 *       constructs an {@link org.azertio.core.expressions.ExpressionASTNode} tree from the tokens</li>
 *   <li><strong>Matcher Building</strong> - {@link org.azertio.core.expressions.ExpressionMatcherBuilder}
 *       creates an {@link org.azertio.core.expressions.ExpressionMatcher} from the AST</li>
 * </ol>
 *
 * <h2>Expression Syntax</h2>
 *
 * <ul>
 *   <li><strong>Literal text</strong> - Plain text matches exactly</li>
 *   <li><strong>Optional</strong> - {@code (optional text)} may or may not be present</li>
 *   <li><strong>Choice</strong> - {@code word1|word2} or {@code (phrase1|phrase2)}</li>
 *   <li><strong>Negation</strong> - {@code ^word} or {@code ^[phrase]} matches anything except</li>
 *   <li><strong>Wildcard</strong> - {@code *} matches any text</li>
 *   <li><strong>Arguments</strong> - {@code {type}} or {@code {name:type}} for typed parameters</li>
 *   <li><strong>Assertions</strong> - {@code {{assertion-name}}} for validation patterns</li>
 * </ul>
 *
 * <h2>Main Classes</h2>
 *
 * <h3>Tokenization</h3>
 * <ul>
 *   <li>{@link org.azertio.core.expressions.ExpressionTokenType} - Enum of token types</li>
 *   <li>{@link org.azertio.core.expressions.ExpressionToken} - Token with type, value, and position</li>
 *   <li>{@link org.azertio.core.expressions.ExpressionTokenizer} - Lexical analyzer</li>
 * </ul>
 *
 * <h3>AST</h3>
 * <ul>
 *   <li>{@link org.azertio.core.expressions.ExpressionASTNode} - AST node with type and children</li>
 *   <li>{@link org.azertio.core.expressions.ExpressionASTBuilder} - Parser that builds AST</li>
 * </ul>
 *
 * <h3>Matching</h3>
 * <ul>
 *   <li>{@link org.azertio.core.expressions.ExpressionMatcher} - Main expression matcher</li>
 *   <li>{@link org.azertio.core.expressions.ExpressionMatcherBuilder} - Builds matchers from expressions</li>
 *   <li>{@link org.azertio.core.expressions.FragmentMatcher} - Interface for fragment matching</li>
 *   <li>{@link org.azertio.core.expressions.PatternFragmentMatcher} - Regex-based matching</li>
 *   <li>{@link org.azertio.core.expressions.ArgumentFragmentMatcher} - Typed argument matching</li>
 *   <li>{@link org.azertio.core.expressions.AssertionFactoryFragmentMatcher} - Assertion matching</li>
 * </ul>
 *
 * <h3>Values</h3>
 * <ul>
 *   <li>{@link org.azertio.core.expressions.Match} - Result of expression matching</li>
 *   <li>{@link org.azertio.core.expressions.ArgumentValue} - Sealed interface for argument values</li>
 *   <li>{@link org.azertio.core.expressions.LiteralValue} - Literal argument value</li>
 *   <li>{@link org.azertio.core.expressions.VariableValue} - Variable reference value</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * // Build the matcher
 * ExpressionMatcherBuilder builder = new ExpressionMatcherBuilder(dataTypes, assertions);
 * ExpressionMatcher matcher = builder.buildExpressionMatcher(
 *     "the user {name:text} has {count:number} items"
 * );
 *
 * // Match against input
 * Match match = matcher.matches("the user John has 5 items", Locale.ENGLISH);
 *
 * if (match.matched()) {
 *     ArgumentValue name = match.argument("name");
 *     ArgumentValue count = match.argument("count");
 * }
 * }</pre>
 *
 * @author Luis Iñesta Gelabert
 * @see org.azertio.core.expressions.ExpressionMatcher
 * @see org.azertio.core.expressions.ExpressionMatcherBuilder
 */
package org.azertio.core.expressions;