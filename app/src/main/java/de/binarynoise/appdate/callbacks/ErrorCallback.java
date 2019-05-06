package de.binarynoise.appdate.callbacks;

@FunctionalInterface
public interface ErrorCallback {
	void onError(Throwable t);
}
