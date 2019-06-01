package de.binarynoise.auth;

public class Tokens {
	static {
		System.loadLibrary("native-lib");
		getGoogleCred();
	}
	
	public static String getGoogleCred() {
		return getGoogleCredNative();
	}
	
	private static native String getGoogleCredNative();
}
