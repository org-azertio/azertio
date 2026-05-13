package org.azertio.core.util;

/**
 * @author Luis Iñesta Gelabert - luiinge@gmail.com
 */
@FunctionalInterface
public interface ThrowableRunnable {

	void run(Object... arguments) throws Exception;

}
