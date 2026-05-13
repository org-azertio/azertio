package org.azertio.core.datatypes;

import org.azertio.core.AzertioException;
import org.azertio.core.DataType;
import org.azertio.core.util.Patterns;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Abstract base class for implementing {@link DataType} with regex-based validation.
 *
 * <p>This adapter provides a template for creating data types that:</p>
 * <ul>
 *   <li>Match input values against a regex pattern</li>
 *   <li>Validate input before parsing</li>
 *   <li>Provide descriptive error messages on validation failure</li>
 * </ul>
 *
 * <h2>Example Implementation</h2>
 * <pre>{@code
 * public class EmailDataTypeAdapter extends DataTypeAdapter<String> {
 *     public EmailDataTypeAdapter() {
 *         super(
 *             "email",
 *             String.class,
 *             "[\\w.+-]+@[\\w.-]+\\.[a-zA-Z]{2,}",
 *             "user@example.com",
 *             x -> x
 *         );
 *     }
 * }
 * }</pre>
 *
 * @param <T> the Java type that this adapter produces when parsing
 * @see RegexDataTypeAdapter
 * @see DataType

 * @author Luis Iñesta Gelabert - luiinge@gmail.com */
public abstract class DataTypeAdapter<T> implements DataType {


	private final String name;
	private final Class<T> javaType;
	private final Pattern pattern;
	private final String hint;
	private final Function<String,T> parser;


	/**
	 * Creates a new data type adapter.
	 *
	 * @param name    the unique name of this data type (e.g., "number", "date")
	 * @param javaType the Java class that this data type produces
	 * @param regex   the regex pattern for matching valid input
	 * @param hint    a human-readable hint showing the expected format
	 * @param parser  the function to convert a matched string into the target type
	 */
	protected DataTypeAdapter(
		String name,
		Class<T> javaType,
		String regex,
		String hint,
		Function<String,T> parser
	) {
		this.name = name;
		this.javaType = javaType;
		this.pattern = Patterns.of(regex);
		this.hint = hint;
		this.parser = parser;
	}


	@Override
	public String name() {
		return name;
	}

	@Override
	public Class<?> javaType() {
		return javaType;
	}

	@Override
	public String hint() {
		return hint;
	}

	@Override
	public Pattern pattern() {
		return pattern;
	}

	@Override
	public T parse(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		Matcher matcher = matcher(value);
		if (!matcher.matches()) {
			throw new AzertioException("Invalid value for data type {}: {}\nExpected pattern: {}",name,value,hint);
		}
		return parser.apply(value);
	}
}
