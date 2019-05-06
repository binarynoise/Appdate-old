package de.binarynoise.appdate.callbacks;

@FunctionalInterface
public interface Result<T> {
	/**
	 * onResult should always call successCallback.onSuccess at the end
	 */
	void onResult(T result, SuccessCallback successCallback, ErrorCallback errorCallback);
}
