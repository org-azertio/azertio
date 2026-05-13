package org.azertio.core.util;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;


/**
 * @author Luis Iñesta Gelabert - luiinge@gmail.com
 */
public class Lazy<T> {

	public static <T> Lazy<T> of(Supplier<T> supplier) {
		return new Lazy<>(supplier);
	}

	public static <T> Lazy<T> ofOptional(Supplier<Optional<T>> supplier) {
		return new Lazy<>(supplier).map(Optional::orElseThrow);
	}

	private final Supplier<T> supplier;
	private T instance;

	private Lazy(Supplier<T> supplier) {
		this.supplier = supplier;
	}

	public T get() {
		if (instance == null) {
			instance = supplier.get();
		}
		return instance;
	}


	public void reset() {
		instance = null;
	}


	public <U> Lazy<U> map(Function<T,U> function) {
		return new Lazy<>(()->function.apply(supplier.get()));
	}


}
