package de.binarynoise.appdate.util;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Annotation for methods that do network operation
 * and which must not be called from the android main thread
 */
@Documented
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface RunInBackground {}
