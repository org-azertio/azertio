package org.azertio.core.util;


import java.lang.reflect.InvocationTargetException;
import java.util.function.*;
import java.util.stream.Stream;

import org.slf4j.*;


/**
 * @author Luis Iñesta Gelabert - luiinge@gmail.com
 */
public class Log {

	private static final Log singleton = new Log("org.azertio");


	public static Log of() {
		return singleton;
	}

	public static Log of(String category) {
		return new Log("org.azertio."+category);
	}


	private final Logger delegate;

	private Log(String category) {
		this.delegate = LoggerFactory.getLogger(category);
	}


	public void error(String message) {
		delegate.error(message);
	}

	public void error(String message, Object... arguments) {
		delegate.error(message, arguments);
	}

	public void error(String message, Supplier<?>... arguments) {
		if (delegate.isErrorEnabled()) {
			var argumentValues = Stream.of(arguments).map(Supplier::get).toArray();
			delegate.error(message, argumentValues);
		}
	}

	public void error(Throwable throwable) {
		throwable = getCause(throwable);
		delegate.error(throwable.getMessage());
		if (delegate.isDebugEnabled()) {
			delegate.debug("",throwable);
		}
	}

	public void error(Throwable throwable, String message) {
		throwable = getCause(throwable);
		String error = message+": "+throwable.getMessage();
		delegate.error(error);
		if (delegate.isDebugEnabled()) {
			delegate.debug("",throwable);
		}
	}

	public void error(Throwable throwable, String message, Object... arguments) {
		throwable = getCause(throwable);
		String error = message+": "+throwable.getMessage();
		delegate.error(error,arguments);
		if (delegate.isDebugEnabled()) {
			delegate.debug("",throwable);
		}
	}


	public void error(Throwable throwable, String message, Supplier<?>... arguments) {
		throwable = getCause(throwable);
		if (delegate.isErrorEnabled()) {
			String error = message+": "+throwable.getMessage();
			var argumentValues = Stream.of(arguments).map(Supplier::get).toArray();
			delegate.error(error, argumentValues);
		}
		if (delegate.isDebugEnabled()) {
			delegate.debug("",throwable);
		}
	}

	
	public void warn(String message) {
		delegate.warn(message);
	}

	public void warn(String message, Object... arguments) {
		delegate.warn(message, arguments);
	}
	
	public void warn(String message, Supplier<?>... arguments) {
		if (delegate.isWarnEnabled()) {
			var argumentValues = Stream.of(arguments).map(Supplier::get).toArray();
			delegate.warn(message, argumentValues);
		}
	}

	
	public void info(String message) {
		delegate.info(message);
	}

	public void info(String message, Object... arguments) {
		delegate.info(message, arguments);
	}

	public void info(String message, Supplier<?>... arguments) {
		if (delegate.isInfoEnabled()) {
			var argumentValues = Stream.of(arguments).map(Supplier::get).toArray();
			delegate.info(message, argumentValues);
		}
	}


	public void debug(String message) {
		delegate.debug(message);
	}

	public void debug(String message, Object... arguments) {
		delegate.debug(message, arguments);
	}

	public void debug(String message, Supplier<?>... arguments) {
		if (delegate.isDebugEnabled()) {
			var argumentValues = Stream.of(arguments).map(Supplier::get).toArray();
			delegate.debug(message, argumentValues);
		}
	}


	public void trace(String message) {
		delegate.trace(message);
	}

	public void trace(String message, Object... arguments) {
		delegate.trace(message, arguments);
	}

	public void trace(String message, Supplier<?>... arguments) {
		if (delegate.isTraceEnabled()) {
			var argumentValues = Stream.of(arguments).map(Supplier::get).toArray();
			delegate.trace(message, argumentValues);
		}
	}
	
	
	private static Throwable getCause(Throwable throwable) {
		if (throwable instanceof InvocationTargetException e) {
			throwable = e.getTargetException();
		}
		if (throwable.getCause() != null && throwable.getCause() != throwable) {
			return getCause(throwable.getCause());
		} else {
			return throwable;
		}
	}




}
