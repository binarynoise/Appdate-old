package de.binarynoise.appdate;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

import de.binarynoise.appdate.callbacks.ErrorCallback;
import de.binarynoise.appdate.callbacks.ProgressCallback;
import de.binarynoise.appdate.callbacks.SuccessCallback;
import de.binarynoise.appdate.callbacks.UpToDateCallback;
import de.binarynoise.appdate.util.*;
import net.erdfelt.android.apk.AndroidApk;

import static android.content.pm.PackageInstaller.*;
import static de.binarynoise.appdate.SFC.sfcm;

@SuppressWarnings("WeakerAccess")
public class App {
	public static final String                 TAG                   = "App";
	public final        URL                    updateUrl;
	public final        String                 downloadURLString;
	public              String                 installedName;
	public              Version                updateVersion;
	@Nullable
	public              String                 installedPackageName;
	public              boolean                hasUpdates;
	public              long                   lastUpdated;
	public              String                 cachePath;
	public              long                   cacheFileSize         = -1;
	@Nullable
	public              Version                installedVersion;
	public transient    InstallState           installState          = InstallState.notInstalling;
	public transient    Download               downloadState         = Download.notDownloading;
	public transient    ProgressCallback       downloadProgressCallback;
	public transient    SuccessCallback        downloadSuccessCallback;
	public transient    ErrorCallback          downloadErrorCallback;
	public transient    SuccessCallback        installSuccessCallback;
	public transient    ErrorCallback          installErrorCallback;
	public transient    UpToDateCallback       installUpToDateCallback;
	public transient    long                   lastCheckedForUpdates = -1;
	public transient    Tupel<Version, String> lastVersion;
	
	@RunInBackground
	public App(String installedName, URL updateUrl) throws IOException {
		this.installedName = installedName.trim();
		this.updateUrl = updateUrl;
//		isInstalled = false;
		
		Tupel<Version, String> t = getLatestVersionCode();
		updateVersion = t._1;
		downloadURLString = t._2;
		hasUpdates = true;
		installedVersion = new Version("");
	}
	
	@RunInBackground
	public App(PackageInfo packageInfo, PackageManager pm, URL updateUrl) throws IOException {
		installedName = String.valueOf(packageInfo.applicationInfo.loadLabel(pm));
		installedPackageName = packageInfo.packageName;
		this.updateUrl = updateUrl;
		installedVersion = new Version("");
		installedVersion = new Version(packageInfo.versionName);
		lastUpdated = packageInfo.lastUpdateTime;
//		isInstalled = true;
		
		Tupel<Version, String> t = getLatestVersionCode();
		updateVersion = t._1;
		downloadURLString = t._2;
		hasUpdates = false;
	}
	
	@SuppressWarnings("HardcodedFileSeparator")
	public static String toAbsolutePath(URL base, String rel) throws MalformedURLException {
		return !rel.isEmpty() && rel.charAt(0) == '/' ? new URL(base, rel).toString() : rel;
	}
	
	public String getLastUpdatedString() {
		return Util.getDateTimeStringForInstant(lastUpdated);
	}
	
	public String getInstalledVersionString() {
		if (!isInstalled())
			installedVersion = null;
		return installedVersion == null ? "" : installedVersion.toString();
	}
	
	public boolean isInstalled() {
		Context context = sfcm.sfc.getContext();
		if (installedPackageName == null || installedPackageName.isEmpty())
			return false;
		PackageManager packageManager = context.getPackageManager();
		try {
			PackageInfo packageInfo = packageManager.getPackageInfo(installedPackageName, 0);
			installedVersion = new Version(packageInfo.versionName);
			return true;
		} catch (PackageManager.NameNotFoundException ignored) {}
		installedVersion = null;
		installedPackageName = null;
		if (isDownloadValid())
			try {
				AndroidApk apk = new AndroidApk(new File(cachePath));
				installedPackageName = apk.getPackageName();
			} catch (IOException ignore) {}
		return false;
	}
	
	@RunInBackground
	public void download() {
		try {
			if (!checkForUpdates()) {
				downloadErrorCallback.onError(
					new InstallNotPermittedException(sfcm.sfc.getContext().getString(R.string.appDetail_updateUpToDate)));
				return;
			}
		} catch (IOException e) {
			downloadErrorCallback.onError(e);
			return;
		}
		
		if (isDownloadValid()) {
			AndroidApk apk;
			try {
				apk = new AndroidApk(new File(cachePath));
			} catch (IOException e) {
				downloadErrorCallback.onError(e);
				return;
			}
			if (new Version(apk.getAppVersion()).compareTo(updateVersion) >= 0) {
				downloadSuccessCallback.onSuccess();
				return;
			}
		}
		
		new Thread(() -> {
			try {
				Tupel<String, Long> t = DownloadManager.downloadToFile(downloadURLString, Util.getFolderForType("apk"),
					(progress, max) -> downloadProgressCallback.onProgress(progress, max));
				cachePath = t._1;
				cacheFileSize = t._2;
			} catch (IOException e) {
				downloadErrorCallback.onError(e);
				return;
			}
			
			if (installedPackageName == null || installedPackageName.isEmpty()) {
				AndroidApk apk;
				try {
					apk = new AndroidApk(new File(cachePath));
				} catch (IOException e) {
					downloadErrorCallback.onError(e);
					return;
				}
				installedPackageName = apk.getPackageName();
				downloadSuccessCallback.onSuccess();
			}
		}).start();
	}
	
	@RunInBackground
	public boolean checkForUpdates() throws IOException {
		updateVersion = getLatestVersionCode()._1;
		hasUpdates = installedVersion == null || !isInstalled() || updateVersion.isNewer(installedVersion);
		return hasUpdates;
	}
	
	public void install() {
		Installer.install(this);
	}
	
	public String setInstalledName(String newInstalledName) {
		if (!isInstalled())
			installedName = newInstalledName;
		return installedName;
	}
	
	public void delete() {
		//TODO
	}
	
	@RunInBackground
	public Tupel<Version, String> getLatestVersionCode() throws IOException {
		if (System.currentTimeMillis() - lastCheckedForUpdates < 2000 && lastVersion != null && lastVersion._1 != null)
			return lastVersion;
		
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(updateUrl.openStream()))) {
			String line;
			while ((line = reader.readLine()) != null)
				if (line.contains("apk") && line.contains("href")) {
					String[] htmlSplits = line.trim().split("\"");
					for (String path : htmlSplits)
						if (path.split("\\?")[0].endsWith(".apk")) {
							String[] pathSplits = path.split("/");
							String fileName = pathSplits[pathSplits.length - 1];
							if (fileName.endsWith(".apk") && fileName.matches(".*\\d+.*")) {
								String substring = fileName.substring(0, fileName.length() - 4);
								Version version = new Version(substring);
								lastVersion = new Tupel<>(version, toAbsolutePath(updateUrl, path));
								lastCheckedForUpdates = System.currentTimeMillis();
								return lastVersion;
							}
						}
				}
		}
		Log.e(TAG, "no releases found");
		throw new IOException(sfcm.sfc.getContext().getString(R.string.err_noReleases));
	}
	
	public boolean isDownloadValid() {
		if (cachePath == null || cachePath.isEmpty())
			return false;
		else if (cacheFileSize == 0) {
			deleteCacheFile();
			return false;
		}
		File file = new File(cachePath);
		if (!file.exists())
			return false;
		long size = file.length();
		boolean valid = cacheFileSize == size;
		if (!valid)
			deleteCacheFile();
		return valid;
	}
	
	public void deleteCacheFile() {
		if (cachePath.isEmpty())
			return;
		File file = new File(cachePath);
		if (!file.exists() || file.delete()) {
			cachePath = "";
			cacheFileSize = 0;
		}
	}
	
	void onInstallStateChange(Bundle bundle) {
		int state = bundle.getInt(EXTRA_STATUS);
		if (state == STATUS_SUCCESS) {
			installSuccessCallback.onSuccess();
			installedVersion = updateVersion;
			deleteCacheFile();
			installState = InstallState.successfull;
		} else if (state > STATUS_SUCCESS) {
			String message = bundle.getString(EXTRA_STATUS_MESSAGE);
			if (message != null)
				installErrorCallback.onError(new InstallNotPermittedException(message));
			else
				installErrorCallback.onError(
					new InstallNotPermittedException(sfcm.sfc.getContext().getString(R.string.appDetail_installFailed)));
		} else
			installState = InstallState.pending;
	}
	
	Drawable getIcon() {
		PackageManager packageManager = sfcm.sfc.getContext().getPackageManager();
		try {
			PackageInfo packageInfo = packageManager.getPackageInfo(installedPackageName, 0);
			return packageInfo.applicationInfo.loadIcon(packageManager);
		} catch (PackageManager.NameNotFoundException e) {
			return new ColorDrawable(Color.TRANSPARENT);
		}
	}
	
	public enum InstallState {
		notInstalling, pending, installing, successfull, failed
	}
	
	public enum Download {
		notDownloading, pending, downloading, complete, failed, cancelling, cancelled
	}
}
