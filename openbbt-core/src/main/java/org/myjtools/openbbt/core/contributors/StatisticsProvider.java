package org.myjtools.openbbt.core.contributors;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Luis Iñesta Gelabert - luiinge@gmail.com
 *
 * This annotation is used to mark methods that provide statistics for the OpenBBT framework.
 * Methods annotated with @StatisticsProvider are responsible of gathering and reporting statistics
 * during the execution of the test suite.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface StatisticsProvider {
}
