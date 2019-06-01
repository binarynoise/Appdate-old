package de.binarynoise.appdate.app;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import androidx.annotation.Nullable;

import java.io.*;
import java.net.URL;
import java.security.SecureRandom;

import de.binarynoise.appdate.DownloadManagerService;
import de.binarynoise.appdate.Installer;
import de.binarynoise.appdate.R;
import de.binarynoise.appdate.callbacks.ErrorCallback;
import de.binarynoise.appdate.callbacks.ProgressCallback;
import de.binarynoise.appdate.callbacks.SuccessCallback;
import de.binarynoise.appdate.callbacks.UpToDateCallback;
import de.binarynoise.appdate.util.*;
import net.erdfelt.android.apk.AndroidApk;

import static android.content.pm.PackageInstaller.*;
import static de.binarynoise.appdate.SFC.sfcm;

@SuppressWarnings({"WeakerAccess", "FieldHasSetterButNoGetter"})
public class App {
	public static final         String                 TAG                   = "App";
	public final                URL                    updateUrl;
	public final                int                    id                    = generateID();
	public                      String                 downloadURLString;
	public                      String                 installedName;
	public                      Version                updateVersion;
	@Nullable public            String                 installedPackageName;
	public                      boolean                hasUpdates;
	public                      long                   lastUpdated;
	public                      String                 cachePath;
	public                      long                   cacheFileSize         = -1;
	@Nullable public            Version                installedVersion;
	public transient            long                   lastCheckedForUpdates = -1;
	public transient            Tupel<Version, String> lastVersion;
	private transient           InstallState           installState          = InstallState.notInstalling;
	@Nullable private transient Throwable              installFailedReason;
	private transient           Download               downloadState         = Download.notDownloading;
	@Nullable private transient Throwable              downloadFailedReason;
	private transient           ProgressCallback       downloadProgressCallback;
	private transient           SuccessCallback        downloadSuccessCallback;
	private transient           ErrorCallback          downloadErrorCallback;
	private transient           SuccessCallback        installSuccessCallback;
	private transient           ErrorCallback          installErrorCallback;
	private transient           UpToDateCallback       installUpToDateCallback;
	
	@RunInBackground
	public App(String installedName, URL updateUrl) throws IOException {
		this.installedName = installedName.trim();
		this.updateUrl = updateUrl;
		
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
		installedVersion = new Version(packageInfo.versionName);
		lastUpdated = packageInfo.lastUpdateTime;
		
		Tupel<Version, String> t = getLatestVersionCode();
		updateVersion = t._1;
		downloadURLString = t._2;
		hasUpdates = false;
	}
	
	@RunInBackground
	public App(AppTemplate template) throws IOException {
		installedName = template.name;
		installedPackageName = template.packageName;
		updateUrl = template.updateUrl;
		Tupel<Version, String> t = getLatestVersionCode();
		downloadURLString = t._2;
		updateVersion = t._1;
		PackageManager pm = sfcm.sfc.getContext().getPackageManager();
		try {
			PackageInfo packageInfo = pm.getPackageInfo(installedPackageName, 0);
			lastUpdated = packageInfo.lastUpdateTime;
		} catch (PackageManager.NameNotFoundException ignored) {}
	}
	
	private static int generateID() {
		int id;
		do
			id = new SecureRandom().nextInt();
		while ((id == 0 || id == -1));
		return id;
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
			installedName = packageManager.getApplicationLabel(packageInfo.applicationInfo).toString();
			lastUpdated = packageInfo.lastUpdateTime;
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
		Context context = sfcm.sfc.getContext();
		try {
			if (!checkForUpdates()) {
				onInstallUpToDate();
				return;
			}
		} catch (IOException e) {
			onDownloadError(e);
			return;
		}
		
		if (isDownloadValid()) {
			AndroidApk apk;
			try {
				apk = new AndroidApk(new File(cachePath));
			} catch (IOException e) {
				onDownloadError(e);
				return;
			}
			if (new Version(apk.getAppVersion()).compareTo(updateVersion) >= 0) {
				onDownloadSuccess();
				return;
			}
		}
		
		setDownloadState(Download.pending);
		
		JobScheduler jobScheduler = (JobScheduler) (context.getSystemService(Context.JOB_SCHEDULER_SERVICE));
		if (jobScheduler != null) {
			JobInfo jobInfo = new JobInfo.Builder(id, new ComponentName(sfcm.sfc.getContext(), DownloadManagerService.class))
				.setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
				.setBackoffCriteria(JobInfo.MAX_BACKOFF_DELAY_MILLIS, JobInfo.BACKOFF_POLICY_EXPONENTIAL)
				.setPersisted(true)
				.build();
			jobInfo.getExtras().putInt(DownloadManagerService.EXTRA_APP_ID, id);
			jobScheduler.schedule(jobInfo);
		}
	}
	
	@RunInBackground
	public boolean checkForUpdates() throws IOException {
		updateVersion = getLatestVersionCode()._1;
		downloadURLString = getLatestVersionCode()._2;
		hasUpdates = installedVersion == null || !isInstalled() || updateVersion.isNewer(installedVersion);
		return hasUpdates;
	}
	
	public void install() {
		if (!hasUpdates) {
			onInstallUpToDate();
			return;
		}
		
		if (!isDownloadValid()) {
			if (cachePath != null && !cachePath.isEmpty())
				onInstallError(new FileNotFoundException(
					"the downloaded file has either been deleted or is invalid: '" + cachePath + "'")); //TODO
			else
				onInstallError(new FileNotFoundException("you need to download the apk before installing")); //TODO
			return;
		}
		
		File apkfile = new File(cachePath);
		AndroidApk apk;
		try {
			apk = new AndroidApk(apkfile);
		} catch (IOException e) {
			onInstallError(e);
			return;
		}
		
		String updatePackageName = apk.getPackageName();
		if (!updatePackageName.equalsIgnoreCase(installedPackageName)) {
			onInstallError(new InstallNotPermittedException("Packagename mismatch"));
			return;
		}

//		updateVersion = new Version(apk.getAppVersion());
//		if (installedVersion != null && !updateVersion.isNewer(installedVersion)) {
//			onInstallUpToDate();
//			return;
//		}
		setInstallState(InstallState.pending);
		
		Installer.install(apkfile, this::onDownloadError);
	}
	
	public String setInstalledName(String newInstalledName) {
		if (!isInstalled())
			installedName = newInstalledName;
		return installedName;
	}
	
	public void delete() {
		deleteCacheFile();
		AppList.removeFromList(this);
	}
	
	@RunInBackground
	public Tupel<Version, String> getLatestVersionCode() throws IOException {
		if (System.currentTimeMillis() - lastCheckedForUpdates < 2000 && lastVersion != null && lastVersion._1 != null)
			return lastVersion;
		
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(updateUrl.openStream()))) {
			String line;
			while ((line = reader.readLine()) != null) // iterate through html line by line
				if (line.contains(".apk") && line.contains("href")) { // find lines containing links to apk-files
					String[] htmlSplits = line.trim().split("\""); // find the link within the line
					for (String path : htmlSplits) {
						path = path.split("\\?")[0];
						if (path.endsWith(".apk")) {
							String[] pathSplits = path.split("/"); // find the version within the path
							for (int i = pathSplits.length - 1; i >= 0; i--) {
								String fileName = pathSplits[i];
								if (fileName.matches(".*\\d+.*")) {
									if (fileName.endsWith(".apk")) // split off file type from version
										fileName = fileName.substring(0, fileName.length() - 4);
									Version version = new Version(fileName);
									lastVersion = new Tupel<>(version, Util.toAbsolutePath(updateUrl, path)); // cache
									lastCheckedForUpdates = System.currentTimeMillis();
									return lastVersion;
								}
							}
						}
					}
				}
		}
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
		if (!file.exists()) {
			deleteCacheFile();
			return false;
		}
		long size = file.length();
		boolean valid = cacheFileSize == size;
		if (!valid)
			deleteCacheFile();
		return valid;
	}
	
	public void deleteCacheFile() {
		if (cachePath == null || cachePath.isEmpty())
			return;
		File file = new File(cachePath);
		if (!file.exists() || file.delete()) {
			cachePath = "";
			cacheFileSize = 0;
		}
	}
	
	public void onInstallStateChange(Bundle bundle) {
		int state = bundle.getInt(EXTRA_STATUS);
		if (state == STATUS_SUCCESS)
			onInstallSuccess();
		else if (state > STATUS_SUCCESS) {
			String message = bundle.getString(EXTRA_STATUS_MESSAGE);
			if (message != null && !message.isEmpty())
				onInstallError(new InstallNotPermittedException(message));
			else
				onInstallError(new InstallNotPermittedException(sfcm.sfc.getContext().getString(R.string.err_installFailed)));
		} else
			setInstallState(InstallState.pending);
	}
	
	public Drawable getIcon() {
		PackageManager packageManager = sfcm.sfc.getContext().getPackageManager();
		try {
			PackageInfo packageInfo = packageManager.getPackageInfo(installedPackageName, 0);
			return packageInfo.applicationInfo.loadIcon(packageManager);
		} catch (PackageManager.NameNotFoundException e) {
			return new ColorDrawable(Color.TRANSPARENT);
		}
	}
	
	public void setDownloadProgressCallback(ProgressCallback downloadProgressCallback) {
		this.downloadProgressCallback = downloadProgressCallback;
	}
	
	public void setDownloadSuccessCallback(SuccessCallback downloadSuccessCallback) {
		this.downloadSuccessCallback = downloadSuccessCallback;
	}
	
	public void setDownloadErrorCallback(ErrorCallback downloadErrorCallback) {
		this.downloadErrorCallback = downloadErrorCallback;
	}
	
	public void setInstallSuccessCallback(SuccessCallback installSuccessCallback) {
		this.installSuccessCallback = installSuccessCallback;
	}
	
	public void setInstallErrorCallback(ErrorCallback installErrorCallback) {
		this.installErrorCallback = installErrorCallback;
	}
	
	public void setInstallUpToDateCallback(UpToDateCallback installUpToDateCallback) {
		this.installUpToDateCallback = installUpToDateCallback;
	}
	
	public void onDownloadProgress(long progress, long max) {
		setDownloadState(Download.downloading);
		downloadProgressCallback.onProgress(progress, max);
	}
	
	public void onDownloadSuccess() {
		setDownloadState(Download.complete);
		downloadSuccessCallback.onSuccess();
	}
	
	public void onDownloadError(Throwable t) {
		setDownloadFailed(t);
		downloadErrorCallback.onError(t);
	}
	
	public void onInstallSuccess() {
		setInstallState(InstallState.successfull);
		installSuccessCallback.onSuccess();
		installedVersion = updateVersion;
		lastUpdated = System.currentTimeMillis();
		deleteCacheFile();
		isInstalled();
	}
	
	public void onInstallError(Throwable t) {
		setInstallFailed(t);
		installErrorCallback.onError(t);
	}
	
	public void onInstallUpToDate() {
		setInstallState(InstallState.notInstalling);
		installUpToDateCallback.onUpToDate();
	}
	
	public void onResult(Tupel<String, Long> t, SuccessCallback successCallback, ErrorCallback errorCallback) {
		cachePath = t._1;
		cacheFileSize = t._2;
		
		if (installedPackageName == null || installedPackageName.isEmpty())
			try {
				AndroidApk apk = new AndroidApk(new File(cachePath));
				installedPackageName = apk.getPackageName();
				successCallback.onSuccess();
			} catch (IOException e) {
				errorCallback.onError(e);
			}
		else
			successCallback.onSuccess();
	}
	
	public InstallState getInstallState() {
		return installState;
	}
	
	private void setInstallState(InstallState installState) {
		if (installState == InstallState.failed)
			throw new IllegalArgumentException("for Install.failed use setInstallFailed(cause) instead");
		this.installState = installState;
		installFailedReason = null;
	}
	
	@Nullable
	public Throwable getInstallFailedReason() {
		return installFailedReason;
	}
	
	@Nullable
	public Throwable getDownloadFailedReason() {
		return downloadFailedReason;
	}
	
	public Download getDownloadState() {
		return downloadState;
	}
	
	private void setDownloadState(Download downloadState) {
		if (downloadState == Download.failed)
			throw new IllegalArgumentException("for Download.failed use setDownloadFailed(cause) instead");
		this.downloadState = downloadState;
		downloadFailedReason = null;
	}
	
	private void setInstallFailed(Throwable t) {
		installState = InstallState.failed;
		installFailedReason = t;
	}
	
	private void setDownloadFailed(Throwable cause) {
		downloadState = Download.failed;
		downloadFailedReason = cause;
	}
	
	public enum InstallState {
		notInstalling, pending, installing, successfull, failed
	}
	
	public enum Download {
		notDownloading, pending, downloading, complete, failed, cancelling, cancelled
	}
}
