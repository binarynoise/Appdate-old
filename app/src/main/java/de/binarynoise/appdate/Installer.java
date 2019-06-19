package de.binarynoise.appdate;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageInstaller.SessionParams;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import androidx.core.content.FileProvider;

import java.io.*;

import de.binarynoise.appdate.app.App;
import de.binarynoise.appdate.app.AppList;
import de.binarynoise.appdate.util.InstallNotPermittedException;
import de.binarynoise.appdate.util.Util;
import net.erdfelt.android.apk.AndroidApk;

import static android.content.Intent.EXTRA_INTENT;
import static android.content.pm.PackageInstaller.EXTRA_PACKAGE_NAME;
import static android.content.pm.PackageInstaller.SessionParams.MODE_FULL_INSTALL;
import static android.content.pm.PackageManager.INSTALL_REASON_USER;

public class Installer extends BroadcastReceiver {
	@SuppressWarnings("unused") private static final String TAG = "Installer";
	
	public static void install(File apkfile, Context context) throws InstallNotPermittedException, IOException {
		installPackageInstaller(apkfile, context);
	}
	
	@SuppressWarnings("ConstantExpression")
	private static void installPromt(File apkFile, Context context) {
		Intent promptInstall = new Intent(Intent.ACTION_VIEW).setDataAndType(
			FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".fileprovider", apkFile),
			"application/vnd.android.package-archive");
		promptInstall.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		context.startActivity(promptInstall);
	}
	
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	private static void installPackageInstaller(File apkfile, Context context) throws InstallNotPermittedException, IOException {
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
		
		try (PackageInstaller.Session session = packageInstaller.openSession(sessionId)) {
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
	
	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent == null) {
			Util.log(TAG, "intent was null", Log.WARN);
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
			Util.dumpBundle(TAG, extras);
	}
	
	public static class ApkFileProvider extends FileProvider {}
}
