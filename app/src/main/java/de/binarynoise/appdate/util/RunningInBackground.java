package de.binarynoise.appdate.util;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Annotation indicating that network operations are performed within a new thread
 * instead of android's main thread.
 */
@Documented
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface RunningInBackground {}
