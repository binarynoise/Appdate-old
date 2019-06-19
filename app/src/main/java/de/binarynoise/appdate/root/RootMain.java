package de.binarynoise.appdate.root;

import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.List;

import de.binarynoise.appdate.BuildConfig;
import de.binarynoise.appdate.Installer;
import de.binarynoise.appdate.util.InstallNotPermittedException;
import de.binarynoise.appdate.util.Util;
import eu.chainfire.librootjava.Logger;
import eu.chainfire.librootjava.RootIPC;
import eu.chainfire.librootjava.RootJava;
import eu.chainfire.libsuperuser.Debug;

import static de.binarynoise.appdate.util.Util.throwAsRemote;

public class RootMain {
	private static final String TAG = "Appdate:root";
	
	/**
	 * Entry point into code running as root
	 * Execution ends when this method returns
	 *
	 * @param args Passed arguments
	 */
	public static void main(String[] args) {
		// Setup logging - note that these logs do show up in (adb) logcat, but they do not show up
		// in AndroidStudio where the logs from the non-root part of your app are displayed!
		Logger.setLogTag(TAG);
		Logger.setDebugLogging(BuildConfig.DEBUG);
		
		// Setup libsuperuser (required for this example code, but not required to use librootjava in general)
		Debug.setDebug(BuildConfig.DEBUG);
		Debug.setLogTypeEnabled(Debug.LOG_GENERAL | Debug.LOG_COMMAND, true);
		Debug.setLogTypeEnabled(Debug.LOG_OUTPUT, true);
		Debug.setSanityChecksEnabled(false); // don't complain about calls on the main thread
		
		// Log uncaught exceptions rather than just sending them to stderr
		Thread.UncaughtExceptionHandler oldHandler = Thread.getDefaultUncaughtExceptionHandler();
		Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
			Util.log(TAG, String.format("EXCEPTION on thread %s:", thread.getName()), throwable, Log.ERROR);
			if (oldHandler != null)
				oldHandler.uncaughtException(thread, throwable);
			else
				System.exit(1);
		});
		
		// Make sure LD_LIBRARY_PATH is sane again, so we don't run into issues running shell commands
		RootJava.restoreOriginalLdLibraryPath();
		
		// Debugger.waitFor(true);
		
		// Create instance and pass control over to run(), so we don't have to static everything
		// Grab a (limited) context
		Context context = RootJava.getSystemContext();
		
		IBinder ipc = new IIPC.Stub() {
			@Override
			public void install(String apkFile) throws RemoteException {
				try {
					Util.toastAndLog(context, TAG, "installing with root", Toast.LENGTH_SHORT, Log.INFO); //TODO
					Installer.install(new File(apkFile), context);
				} catch (InstallNotPermittedException | IOException e) {
					Log.e(TAG, "install failed", e);
					throwAsRemote(e);
				}
			}
		};
		
		// Send the IPC binder to the non-root part of the app, wait for a connection, and
		// don't return until the app has disconnected.
		try {
			new RootIPC(BuildConfig.APPLICATION_ID, ipc, 0, 10 * 1000, true);
			System.exit(0);
		} catch (RootIPC.TimeoutException e) {
			Util.log(TAG, "non-root process did not connect to IPC", e, Log.ERROR);
			System.exit(1);
		}
	}
	
	/**
	 * Call this from non-root code to generate the script to launch the root code
	 *
	 * @param context Application or activity context
	 * @return Script
	 */
	@SuppressWarnings("StaticMethodOnlyUsedInOneClass")
	static List<String> getLaunchScript(Context context) {
		return RootJava.getLaunchScript(context, RootMain.class, null, null, null, context.getPackageName() + ":root");
	}
}
