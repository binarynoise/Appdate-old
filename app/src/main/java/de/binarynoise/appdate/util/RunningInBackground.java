package de.binarynoise.appdate.util;

import java.lang.annotation.*;

/**
 * Annotation indicating that network operations are performed within a new thread
 * instead of android's main thread.
 */
@Documented
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@Retention(RetentionPolicy.SOURCE)
public @interface RunningInBackground {}
