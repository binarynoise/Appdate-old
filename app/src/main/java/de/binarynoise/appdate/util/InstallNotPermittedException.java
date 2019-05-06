package de.binarynoise.appdate.util;

public class InstallNotPermittedException extends Exception {
	public InstallNotPermittedException(String message) {
		super("App installation not permitted:\n" + message);
	}
	
	@SuppressWarnings("unused")
	public InstallNotPermittedException() {
		super("App installation not permitted");
	}
}
