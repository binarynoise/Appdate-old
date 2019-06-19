package de.binarynoise.appdate.util;

import java.lang.annotation.*;

/**
 * Annotation for methods that do network operations
 * and that must not be called from the android main thread
 */
@Documented
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@Retention(RetentionPolicy.SOURCE)
public @interface RunInBackground {}
