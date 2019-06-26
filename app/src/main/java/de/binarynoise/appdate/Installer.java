package de.binarynoise.appdate;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageInstaller.*;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import de.binarynoise.appdate.app.App;
import de.binarynoise.appdate.app.AppList;
import de.binarynoise.appdate.util.InstallNotPermittedException;
import eu.chainfire.libsuperuser.Shell;
import net.erdfelt.android.apk.AndroidApk;

import static android.content.Intent.EXTRA_INTENT;
import static android.content.pm.PackageInstaller.*;
import static android.content.pm.PackageInstaller.SessionParams.MODE_FULL_INSTALL;
import static android.content.pm.PackageManager.INSTALL_REASON_USER;
import static de.binarynoise.appdate.SFC.sfcm;
import static de.binarynoise.appdate.util.Util.*;

public class Installer extends BroadcastReceiver {
	private static final String TAG = "Installer";
	
	public static void install(String path) throws InstallException, IOException, InstallNotPermittedException {
		try {
			installRoot(sfcm.sfc.getContext(), path);
		} catch (ShellOpenException e) {
			log(TAG, "shell could not be opened", e, Log.WARN);
			installNonRoot(new File(path));
		}
	}
	
	private static void installNonRoot(File apkfile) throws InstallNotPermittedException, IOException {
		Context context = sfcm.sfc.getContext();
		PackageManager packageManager = context.getPackageManager();
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !packageManager.canRequestPackageInstalls())
			throw new InstallNotPermittedException("No permission to request app installs");
		
		String packageName = new AndroidApk(apkfile).getPackageName();
		
		PackageInstaller packageInstaller = packageManager.getPackageInstaller();
		SessionParams params = new SessionParams(MODE_FULL_INSTALL);
		
		int sessionId = packageInstaller.createSession(params);
		
		params.setAppPackageName(packageName);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
			params.setInstallReason(INSTALL_REASON_USER);
		params.setSize(apkfile.length());
		
		try (Session session = packageInstaller.openSession(sessionId)) {
			try (BufferedInputStream bufIn = new BufferedInputStream(new FileInputStream(apkfile));
				OutputStream out = session.openWrite(packageName + ".apk", 0, apkfile.length())) {
				int count;
				byte[] buffer = new byte[0x100000]; // 1MB
				while ((count = bufIn.read(buffer)) >= 0)
					out.write(buffer, 0, count);
				
				out.flush();
				session.fsync(out);
			}
			
			Intent intent = new Intent();
			intent.setComponent(new ComponentName(BuildConfig.APPLICATION_ID, Installer.class.getName()));
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			session.commit(PendingIntent.getBroadcast(context, 0, intent, 0).getIntentSender());
		}
	}
	
	private static void installRoot(Context context, String apkFile) throws ShellOpenException, InstallException, IOException {
		String user = "";
		File externalCacheDir = context.getExternalCacheDir();
		if (externalCacheDir != null) {
			String id = externalCacheDir.getCanonicalPath().split("/")[3]; // /storge/emulated/0
			if (id.matches("\\d+")) {
				int uid = Integer.parseInt(id);
				user = uid >= 0 ? "--user " + uid : "";
			}
		}
		
		if (!Shell.SU.available())
			throw new ShellOpenException();
		
		List<String> result = new ArrayList<>();
		int exitCode;
		try {
			exitCode =
				Shell.Pool.SU.run("pm install -r -i " + BuildConfig.APPLICATION_ID + " " + user + " " + apkFile, result, result,
					false);
		} catch (Shell.ShellDiedException e) {
			ShellOpenException shellOpenException = new ShellOpenException();
			shellOpenException.addSuppressed(e);
			throw shellOpenException;
		}
		
		if (exitCode == 0 && !result.isEmpty() && result.get(0).toLowerCase().startsWith("success")) {
			Intent intent = new Intent();
			intent.setClass(context, Installer.class);
			
			intent.putExtra(EXTRA_PACKAGE_NAME, new AndroidApk(new File(apkFile)).getPackageName());
			intent.putExtra(EXTRA_STATUS, STATUS_SUCCESS);
			
			new Installer().onReceive(sfcm.sfc.getContext(), intent);
			
			return;
		}
		
		log(TAG, String.format(Locale.US, "ExitCode: %d", exitCode));
		logPretty(TAG, result);
		
		if (result.isEmpty())
			throw new ShellOpenException();
		
		String res0 = result.get(0);
		
		if (res0.contains("usage"))
			throw new ShellOpenException();
		
		throw InstallException.parse(res0);
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent == null) {
			log(TAG, "intent was null", Log.WARN);
			return;
		}
		
		Bundle extras = intent.getExtras();
		if (extras == null)
			return;
		
		boolean error = true;
		
		Intent intent2 = (Intent) extras.get(EXTRA_INTENT);
		if (intent2 != null) {
			context.startActivity(intent2);
			context.sendBroadcast(intent2);
			error = false;
		}
		
		String packageName = extras.getString(EXTRA_PACKAGE_NAME);
		if (packageName != null) {
			App app = AppList.findByPackageName(packageName);
			if (app != null) {
				app.onInstallStateChange(extras);
				error = false;
			}
		}
		
		if (error)
			dumpBundle(TAG, extras);
	}
	
	/**
	 * Exception thrown when the root installRoot failed
	 */
	public static class InstallException extends Exception {
		InstallException(String message) {
			super(message);
		}
		
		@SuppressWarnings("StaticMethodOnlyUsedInOneClass")
		static InstallException parse(String log) {
			if (log.startsWith("Failure"))
				return new InstallException(log.substring(log.indexOf('[') + 1, log.length() - 2));
			else if (log.startsWith("Error: "))
				return new InstallException(log.substring(7));
			else
				return new InstallException(log);
		}
	}
	
	/**
	 * Excepion that is thrown when the root shell could not be started properly
	 * or root access was denied and we should fall back to non-root installRoot
	 */
	private static class ShellOpenException extends Exception {}
}
