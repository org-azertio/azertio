package org.azertio.core.util;


import java.util.Optional;
import java.util.function.*;

/**
 * @author Luis Iñesta Gelabert - luiinge@gmail.com
 */
public class Either<T,U>  {

	public static <T,U> Either<T,U> of(Supplier<T> value, Supplier<U> fallback) {
		return new Either<>(value,fallback);
	}

	public static <T,U> Either<T,U> of(Supplier<T> value) {
		return new Either<>(value,()->null);
	}

	public static <T,U> Either<T,U> fallback(Supplier<U> fallback) {
		return new Either<>(()->null,fallback);
	}

	public static <T,U> Either<T,U> of(T value, U fallback) {
		return new Either<>(()->value,()->fallback);
	}

	public static <T,U> Either<T,U> of(T value) {
		return new Either<>(()->value,()->null);
	}

	public static <T,U> Either<T,U> fallback(U fallback) {
		return new Either<>(()->null,()->fallback);
	}

	private final Supplier<T> value;
	private final Supplier<U> fallback;


	private Either(Supplier<T> value, Supplier<U> fallback) {
		this.value = value;
		this.fallback = fallback;
	}


	public Optional<T> value() {
		return Optional.ofNullable(value.get());
	}

	public T value(Function<U,T> mapper) {
		return Optional.ofNullable(value.get()).orElse(mapper.apply(fallback.get()));
	}

	public U fallback() {
		return fallback.get();
	}

	public U fallback(Function<T,U> mapper) {
		return Optional.ofNullable(fallback.get()).orElse(mapper.apply(value.get()));
	}




}
