package de.binarynoise.appdate.util;

@FunctionalInterface
public interface Consumer<T> {
	void accept(T t);
}
