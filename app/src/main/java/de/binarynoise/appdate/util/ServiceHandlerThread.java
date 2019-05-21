package de.binarynoise.appdate.util;

import android.app.Service;
import android.os.HandlerThread;

public class ServiceHandlerThread extends HandlerThread {
	private final Service parent;
	
	public ServiceHandlerThread(String name, int priority, Service parent) {
		super(name, priority);
		this.parent = parent;
	}
	
	ServiceHandlerThread(String name, Service parent) {
		super(name);
		this.parent = parent;
	}
	
	@Override
	public boolean quit() {
		boolean quit = super.quit();
		parent.stopSelf();
		return quit;
	}
	
	@Override
	public boolean quitSafely() {
		boolean quitSafely = super.quitSafely();
		parent.stopSelf();
		return quitSafely;
	}
}
