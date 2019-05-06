package de.binarynoise.appdate.callbacks;

@FunctionalInterface
public interface ProgressCallback {
	void onProgress(long progress, long max);
}
