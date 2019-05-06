package de.binarynoise.appdate;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageInstaller.SessionParams;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import java.io.*;

import de.binarynoise.appdate.util.InstallNotPermittedException;
import de.binarynoise.appdate.util.Util;
import de.binarynoise.appdate.util.Version;
import net.erdfelt.android.apk.AndroidApk;

import static android.content.Intent.EXTRA_INTENT;
import static android.content.pm.PackageInstaller.EXTRA_PACKAGE_NAME;
import static android.content.pm.PackageInstaller.EXTRA_STATUS_MESSAGE;
import static android.content.pm.PackageInstaller.SessionParams.MODE_FULL_INSTALL;
import static android.content.pm.PackageManager.INSTALL_REASON_USER;
import static de.binarynoise.appdate.SFC.sfcm;

public class Installer extends BroadcastReceiver {
	@SuppressWarnings("unused")
	private static final String TAG = "Installer";
	
	@SuppressWarnings({"resource", "IOResourceOpenedButNotSafelyClosed"})
	static void install(App app) {
		/////////////////////////////////// Preparations
		
		if (!app.hasUpdates) {
			app.installUpToDateCallback.onUpToDate();
			return;
		}
		
		if (!app.isDownloadValid()) {
			app.installErrorCallback.onError(
				new FileNotFoundException("the downloaded file has either been deleted or is invalid: '" + app.cachePath + "'"));
			return;
		}
		
		File apkfile = new File(app.cachePath);
		AndroidApk apk;
		try {
			apk = new AndroidApk(apkfile);
		} catch (IOException e) {
			app.installErrorCallback.onError(e);
			return;
		}
		
		String updatePackageName = apk.getPackageName();
		if (!updatePackageName.equalsIgnoreCase(app.installedPackageName)) {
			app.installErrorCallback.onError(new InstallNotPermittedException("Packagename mismatch"));
			return;
		}
		
		app.updateVersion = new Version(apk.getAppVersion());
		if (app.installedVersion != null && !app.updateVersion.isNewer(app.installedVersion)) {
			app.installUpToDateCallback.onUpToDate();
			return;
		}
		
		Context context = sfcm.sfc.getContext();
		PackageManager packageManager = context.getPackageManager();
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !packageManager.canRequestPackageInstalls()) {
			app.installErrorCallback.onError(new InstallNotPermittedException("No permission to request app installs"));
			return;
		}
		
		//////////////////////////////// Installing
		
		String packageName = apk.getPackageName();
		PackageInstaller packageInstaller = packageManager.getPackageInstaller();
		SessionParams params = new SessionParams(MODE_FULL_INSTALL);
		
		int sessionId;
		try {
			sessionId = packageInstaller.createSession(params);
		} catch (IOException e) {
			app.installErrorCallback.onError(e);
			return;
		}
		
		params.setAppPackageName(packageName);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
			params.setInstallReason(INSTALL_REASON_USER);
		params.setSize(apkfile.length());
		
		byte[] buffer = new byte[0x100000]; // 1MB
		OutputStream out = null;
		BufferedInputStream bufIn = null;
		boolean inOpened = false, outOpened = false;
		
		try (PackageInstaller.Session session = packageInstaller.openSession(sessionId)) {
			out = session.openWrite(packageName + ".apk", 0, apkfile.length());
			outOpened = true;
			bufIn = new BufferedInputStream(new FileInputStream(apkfile));
			inOpened = true;
			
			int count;
			while ((count = bufIn.read(buffer)) != -1)
				out.write(buffer, 0, count);
			
			out.flush();
			session.fsync(out);
			
			out.close();
			outOpened = false;
			bufIn.close();
			inOpened = false;
			
			Intent intent = new Intent(context, Installer.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			session.commit(PendingIntent.getBroadcast(context, 0, intent, 0).getIntentSender());
		} catch (IOException e) {
			Log.w(TAG, e);
			app.installErrorCallback.onError(e);
		} finally {
			if (bufIn != null && inOpened)
				try {
					bufIn.close();
				} catch (IOException ignored) {}
			if (out != null && outOpened)
				try {
					out.close();
				} catch (IOException ignored) {}
		}
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent == null)
			return;
		
		Bundle extras = intent.getExtras();
		if (extras == null)
			return;
		
		Util.dumpBundle("BroadcastReciever", extras);
		
		if (extras.containsKey(EXTRA_INTENT)) {
			Intent intent2 = (Intent) extras.get(EXTRA_INTENT);
			if (intent2 != null) {
				context.startActivity(intent2);
				context.sendBroadcast(intent2);
			}
		}
		if (extras.containsKey(EXTRA_STATUS_MESSAGE)) {
			String packageName = extras.getString(EXTRA_PACKAGE_NAME);
			if (packageName != null) {
				App app = sfcm.sfc.appList.find(packageName);
				if (app != null)
					app.onInstallStateChange(extras);
			}
		}
	}
}
