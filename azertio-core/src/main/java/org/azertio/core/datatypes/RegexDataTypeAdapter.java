package org.azertio.core.datatypes;


import org.azertio.core.DataType;
import org.azertio.core.util.Patterns;
import org.azertio.core.util.ThrowableFunction;
import java.util.regex.Pattern;


/**
 * A lightweight {@link DataType} implementation based on regex pattern matching.
 *
 * <p>This adapter is suitable for simple data types where:</p>
 * <ul>
 *   <li>Validation is purely regex-based</li>
 *   <li>Parsing is straightforward</li>
 *   <li>No pre-validation is required before parsing</li>
 * </ul>
 *
 * <p>Unlike {@link DataTypeAdapter}, this class does not validate the input
 * before parsing. Use {@link DataTypeAdapter} if you need automatic validation.</p>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * DataType EMAIL = new RegexDataTypeAdapter<>(
 *     "email",
 *     "[\\w.+-]+@[\\w.-]+\\.[a-zA-Z]{2,}",
 *     String.class,
 *     x -> x,
 *     "user@example.com"
 * );
 * }</pre>
 *
 * @param <T> the Java type that this adapter produces when parsing
 * @see DataTypeAdapter
 * @see DataType

 * @author Luis Iñesta Gelabert - luiinge@gmail.com */
public class RegexDataTypeAdapter<T> implements DataType {

	private final String name;
	private final String regex;
	private final Class<T> javaType;
	private final ThrowableFunction<String,T> parser;
	private final String hint;


	/**
	 * Creates a new regex-based data type adapter.
	 *
	 * @param name     the unique name of this data type
	 * @param regex    the regex pattern for matching valid input
	 * @param javaType the Java class that this data type produces
	 * @param parser   the function to convert a string into the target type
	 * @param hint     a human-readable hint showing the expected format
	 */
	public RegexDataTypeAdapter(
		String name,
		String regex,
		Class<T> javaType,
		ThrowableFunction<String, T> parser,
		String hint
	) {
		this.name = name;
		this.regex = regex;
		this.javaType = javaType;
		this.parser = parser;
		this.hint = hint;
	}


	@Override
	public String name() {
		return name;
	}


	@Override
	public Class<T> javaType() {
		return javaType;
	}


	@Override
	public String hint() {
		return hint;
	}


	@Override
	public Pattern pattern() {
		return Patterns.of(regex);
	}


	@Override
	public Object parse(String value) {
		return parser.apply(value);
	}


	@Override
	public String toString() {
		return name+"["+javaType.getName()+"]";
	}
}
