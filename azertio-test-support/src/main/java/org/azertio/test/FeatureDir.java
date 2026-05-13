package org.azertio.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies the feature directory (relative to {@code /features/} in the test classpath)
 * that an Azertio integration test should load its test plan from.
 * <p>
 * Used in combination with {@link AzertioExtension}.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface FeatureDir {
    String value();
}
