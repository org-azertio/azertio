package org.azertio.core.expressions;

import java.util.*;
import java.util.stream.Stream;


/**
 * Represents a node in the Abstract Syntax Tree (AST) of a parsed expression.
 *
 * <p>The AST is built by {@link ExpressionASTBuilder} from tokens produced by
 * {@link ExpressionTokenizer}. Each node has a type, an optional value, and
 * zero or more child nodes.</p>
 *
 * <h2>Node Types</h2>
 * <ul>
 *   <li>{@link Type#SEQUENCE} - Ordered sequence of child nodes</li>
 *   <li>{@link Type#LITERAL} - Literal text (value contains the text)</li>
 *   <li>{@link Type#CHOICE} - Alternative options (children are the choices)</li>
 *   <li>{@link Type#OPTIONAL} - Optional content (single child)</li>
 *   <li>{@link Type#NEGATION} - Negated content (single child)</li>
 *   <li>{@link Type#WILDCARD} - Matches any text</li>
 *   <li>{@link Type#ARGUMENT} - Typed argument (value is the type name)</li>
 *   <li>{@link Type#ASSERTION} - Assertion reference (value is the assertion name)</li>
 * </ul>
 *
 * @author Luis Iñesta Gelabert - luiinge@gmail.com
 * @see ExpressionASTBuilder
 */
public class ExpressionASTNode {

	/**
	 * Enumeration of AST node types.
	 */
	public enum Type {
		/** Ordered sequence of child nodes. */
		SEQUENCE,
		/** Literal text content. */
		LITERAL,
		/** Alternative choices (children are options). */
		CHOICE,
		/** Wildcard matching any text. */
		WILDCARD,
		/** Assertion reference. */
		ASSERTION,
		/** Typed argument. */
		ARGUMENT,
		/** Optional content. */
		OPTIONAL,
		/** Negated content. */
		NEGATION;


		public ExpressionASTNode empty() {
			return new ExpressionASTNode(this,null);
		}

		public ExpressionASTNode of(ExpressionToken token, ExpressionASTNode... children) {
			return new ExpressionASTNode(this, token == null ? null : token.value() ,children);
		}

		public ExpressionASTNode of(String value, ExpressionASTNode... children) {
			return new ExpressionASTNode(this, value ,children);
		}

		public ExpressionASTNode of(ExpressionASTNode... children) {
			return new ExpressionASTNode(this,null,children);
		}

	}

	final Type type;
	String value;
	final List<ExpressionASTNode> children = new ArrayList<>();



	private ExpressionASTNode(Type type, String value, ExpressionASTNode... children) {
		this.type = type;
		this.value = value;
		Stream.of(children).map(ExpressionASTNode::reduced).forEach(this::add);
	}


	void add(ExpressionASTNode child) {
		children.add(child.reduced());
	}


	public ExpressionASTNode firstChild() {
		return children.isEmpty() ? null : children.getFirst();
	}


	public ExpressionASTNode lastChild() {
		return children.isEmpty() ? null : children.get(children.size()-1);
	}


	public List<ExpressionASTNode> children() {
		return Collections.unmodifiableList(children);
	}

	public Type type() {
		return type;
	}

	public String value() {
		return value;
	}


	public void remove(ExpressionASTNode child) {
		children.remove(child);
	}


	ExpressionASTNode reduced() {
		if (type == Type.SEQUENCE && children.size() == 1) {
			return firstChild();
		} else {
			return this;
		}
	}


	ExpressionASTNode assertType(Type expected) {
		if (this.type != expected)
			throw new ExpressionException("Unexpected node {}; expected {}", this.type, expected);
		return this;
	}



	@Override
	public String toString() {
		return toString(new StringBuilder(),0).toString();
	}


	protected StringBuilder toString(StringBuilder string, int level) {
		String margin = "  ".repeat(level);
		string.append(margin);
		string.append(type);
		if (value != null) {
			string.append("<").append(value).append(">");
		}
		if (!children.isEmpty()) {
			string.append(" [\n");
			children.forEach(child -> child.toString(string, level+1));
			string.append(margin).append("]\n");
		} else {
			string.append("\n");
		}
		return string;
	}



	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ExpressionASTNode astNode = (ExpressionASTNode) o;
		return type == astNode.type
			&& Objects.equals(value, astNode.value)
			&& children.equals(astNode.children);
	}


	@Override
	public int hashCode() {
		return Objects.hash(type, value, children);
	}

}
