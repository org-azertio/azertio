package org.azertio.core.datatypes;


import org.azertio.core.AzertioException;
import org.azertio.core.DataType;

import java.text.*;
import java.util.*;
import java.util.function.Function;

/**
 * Data type adapter for numeric values.
 *
 * <p>This adapter handles parsing of numeric strings with support for:</p>
 * <ul>
 *   <li>Grouping separators (e.g., "1,234,567")</li>
 *   <li>Decimal numbers (e.g., "1,234.56")</li>
 *   <li>Negative numbers (e.g., "-123")</li>
 *   <li>Conversion to different numeric types (Integer, Long, BigDecimal)</li>
 * </ul>
 *
 * <p>The adapter uses English locale for number formatting.</p>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Integer adapter
 * DataType INTEGER = new NumberDataTypeAdapter<>(
 *     "integer", Integer.class, false, false, Number::intValue
 * );
 *
 * // BigDecimal adapter with decimals
 * DataType DECIMAL = new NumberDataTypeAdapter<>(
 *     "decimal", BigDecimal.class, true, true, BigDecimal.class::cast
 * );
 * }</pre>
 *
 * @param <T> the target numeric type
 * @see CoreDataTypes#NUMBER
 * @see CoreDataTypes#BIG_NUMBER
 * @see CoreDataTypes#DECIMAL

 * @author Luis Iñesta Gelabert - luiinge@gmail.com */
public class NumberDataTypeAdapter<T extends Number> extends DataTypeAdapter<T> implements DataType {

	private static final DecimalFormat bigDecimalFormat = (DecimalFormat) NumberFormat.getNumberInstance(Locale.ENGLISH);
	static {
		bigDecimalFormat.setParseBigDecimal(true);
	}
	private static final DecimalFormat regularFormat = (DecimalFormat) NumberFormat.getNumberInstance(Locale.ENGLISH);


	/**
	 * Creates a new numeric data type adapter.
	 *
	 * @param name           the unique name of this data type
	 * @param javaType       the target Java numeric class
	 * @param includeDecimals whether to allow decimal numbers
	 * @param useBigDecimal  whether to parse using BigDecimal precision
	 * @param converter      function to convert parsed Number to target type
	 */
	public NumberDataTypeAdapter(
		String name,
		Class<T> javaType,
		boolean includeDecimals,
		boolean useBigDecimal,
		Function<Number,T> converter
	) {
		super(
			name,
			javaType,
			numericRegexPattern(includeDecimals),
			useBigDecimal ? bigDecimalFormat.toLocalizedPattern() : regularFormat.toLocalizedPattern(),
			parser(includeDecimals, converter)
		);
	}



	private static <T> Function<String,T> parser(boolean includeDecimals, Function<Number, T> converter) {
		return source -> {
			try {
			   var number = includeDecimals ? bigDecimalFormat.parse(source) : regularFormat.parse(source);
			   return converter.apply(number);
			} catch (ParseException e) {
				throw new AzertioException(e);
			}
		};
	}



	private static String numericRegexPattern(boolean includeDecimals) {
		DecimalFormat format = includeDecimals ? bigDecimalFormat : regularFormat;
		DecimalFormatSymbols symbols = format.getDecimalFormatSymbols();
		StringBuilder pattern = new StringBuilder("-?")
			.append("\\d{1,").append(format.getGroupingSize()).append("}")
			.append("(\\").append(symbols.getGroupingSeparator()).append("?")
			.append("\\d{1,").append(format.getGroupingSize()).append("})*");
		if (includeDecimals) {
			pattern.append("\\").append(symbols.getDecimalSeparator()).append("\\d+");
		}
		return pattern.toString();
	}





}
