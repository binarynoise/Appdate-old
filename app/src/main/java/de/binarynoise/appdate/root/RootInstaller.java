package de.binarynoise.appdate.root;

import android.content.Context;
import android.os.RemoteException;
import android.util.Log;

import java.util.List;

import de.binarynoise.appdate.BuildConfig;
import eu.chainfire.librootjava.Debugger;
import eu.chainfire.librootjava.RootIPCReceiver;
import eu.chainfire.libsuperuser.Shell;

import static de.binarynoise.appdate.util.Util.log;

public class RootInstaller {
	private static final String TAG = "RootInstaller";
	
	public static void install(String apkfile, Context context) throws ShellOpenException, RemoteException {
		Debugger.setEnabled(BuildConfig.DEBUG);
		List<String> script = RootMain.getLaunchScript(context);
		
		if (!Shell.SU.available())
			throw new ShellOpenException();
		
		Shell.Interactive shell = new Shell.Builder().useSU().open((commandCode, exitCode, output) -> {});
		
		if (!shell.isRunning())
			throw new ShellOpenException();
		
		shell.addCommand(script, 0, new Shell.OnCommandLineListener() {
			@Override
			public void onLine(String line) {
				log(TAG, line, Log.DEBUG);
			}
			
			@Override
			public void onCommandResult(int commandCode, int exitCode) {
				log(TAG, "exitCode: " + exitCode, Log.DEBUG);
			}
		});
		
		RootIPCReceiver<IIPC> rootIPCReceiver = new RootIPCReceiver<IIPC>(context, 0) {
			@Override
			public void onConnect(IIPC ipc) {
				log(TAG, "connected to root process", Log.INFO);
			}
			
			@Override
			public void onDisconnect(IIPC ipc) {
				log(TAG, "disconnected from root process", Log.INFO);
				shell.kill();
			}
		};
		
		IIPC ipc = rootIPCReceiver.getIPC((BuildConfig.DEBUG ? 60 : 10) * 1000);
		if (ipc != null)
			try {
				ipc.install(apkfile);
			} finally {
				rootIPCReceiver.release();
				shell.kill();
			}
		else {
			log(TAG, "could not connect to root end", Log.WARN);
			throw new ShellOpenException();
		}
	}
	
	public static class ShellOpenException extends Exception {}
}
