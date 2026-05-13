package org.azertio.core.util;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * @author Luis Iñesta Gelabert - luiinge@gmail.com
 */
@EqualsAndHashCode
@ToString
public class Pair<T,U>  {

	public static <T,U> Pair<T,U> of(Supplier<T> value, Supplier<U> fallback) {
		return new Pair<>(value,fallback);
	}

	public static <T,U> Pair<T,U> of(T value, U fallback) {
		return new Pair<>(()->value,()->fallback);
	}

	public static <T,U> List<Pair<T, U>> ofMap(Map<T, U> map) {
		return map.entrySet().stream()
			.map(entry -> new Pair<>(entry::getKey, entry::getValue))
			.toList();
	}


	private final Supplier<T> left;
	private final Supplier<U> right;


	private Pair(Supplier<T> left, Supplier<U> right) {
		this.left = left;
		this.right = right;
	}



	public T left() {
		return left.get();
	}

	public U right() {
		return right.get();
	}



}
