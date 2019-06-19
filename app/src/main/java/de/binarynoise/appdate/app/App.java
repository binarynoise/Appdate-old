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
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import java.io.*;
import java.net.URL;
import java.security.SecureRandom;
import java.util.concurrent.locks.ReentrantLock;

import de.binarynoise.appdate.BuildConfig;
import de.binarynoise.appdate.DownloadManagerService;
import de.binarynoise.appdate.Installer;
import de.binarynoise.appdate.R;
import de.binarynoise.appdate.callbacks.ErrorCallback;
import de.binarynoise.appdate.callbacks.SuccessCallback;
import de.binarynoise.appdate.root.RootInstaller;
import de.binarynoise.appdate.ui.AppDetailActivity;
import de.binarynoise.appdate.ui.AppOverviewFragment;
import de.binarynoise.appdate.util.*;
import net.erdfelt.android.apk.AndroidApk;

import static android.content.pm.PackageInstaller.*;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static de.binarynoise.appdate.SFC.sfcm;
import static de.binarynoise.appdate.util.Util.*;

public class App {
	public static final         String                                  TAG                      = "App";
	public final                int                                     id                       = generateID();
	@NonNull final              URL                                     updateUrl;
	private final transient     ReentrantLock                           lock                     = new ReentrantLock(true);
	public                      String                                  downloadURLString;
	public                      String                                  installedName;
	public                      Version                                 updateVersion;
	@PackagePrivate             boolean                                 hasUpdates;
	@Nullable                   Version                                 installedVersion;
	@Nullable                   String                                  installedPackageName;
	private                     long                                    lastUpdated;
	private                     String                                  cachePath;
	private                     long                                    cacheFileSize            = -1;
	private transient           boolean                                 deleteProgressbarVisible = true;
	@Nullable private transient AppDetailActivity                       myActivity;
	@Nullable private transient AppOverviewFragment.AppOverviewListItem myListItem;
	private transient           long                                    progressBarCurrent       = -1;
	private transient           long                                    progressBarMax           = -1;
	private transient           boolean                                 buttonRowEnabled         = true;
	private transient           long                                    lastCheckedForUpdates    = -1;
	@SuppressWarnings("RedundantFieldInitialization") //
	private transient           Tupel<Version, String>                  lastVersion              = null;
	private transient           boolean                                 updateProgressBarVisible;
	private transient           boolean                                 downloadProgressBarVisible;
	private transient           boolean                                 installProgressBarVisible;
	@SuppressWarnings("RedundantFieldInitialization") //
	private transient           boolean                                 progressBarVisible       = false;
	
	@RunInBackground
	public App(String installedName, @NonNull URL updateUrl) throws IOException {
		this.installedName = installedName.trim();
		this.updateUrl = updateUrl;
		
		Tupel<Version, String> t = getLatestVersionCode();
		updateVersion = t._1;
		downloadURLString = t._2;
		hasUpdates = true;
		installedVersion = new Version("");
	}
	
	@RunInBackground
	public App(PackageInfo packageInfo, PackageManager pm, @NonNull URL updateUrl) throws IOException {
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
	
	/**
	 * private no-args constructor for correct gson deserialization
	 */
	@SuppressWarnings("ALL")
	private App() {
		updateUrl = null;
	}
	
	private static int generateID() {
		int id;
		do
			id = new SecureRandom().nextInt();
		while ((id == 0 || id == -1) && AppList.findById(id) == null);
		return id;
	}
	
	private static String getVersionString(@Nullable Version v) {
		return v == null ? "" : v.toString();
	}
	
	private static void runOnAppOverviewThread(Runnable action) {
		if (sfcm.sfc.mainActivity != null)
			sfcm.sfc.mainActivity.runOnUiThread(action);
	}
	
	@RunInBackground
	public boolean checkForUpdates() throws IOException {
		updateVersion = getLatestVersionCode()._1;
		downloadURLString = getLatestVersionCode()._2;
		hasUpdates = installedVersion == null || !isInstalled() || updateVersion.isNewer(installedVersion);
		return hasUpdates;
	}
	
	public String setInstalledName(String newInstalledName) {
		if (!isInstalled())
			installedName = newInstalledName;
		return installedName;
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
		}
	}
	
	public void onDownloadProgress(long progress, long max) {
		setProgressBar(progress, max);
	}
	
	public void onDownloadSuccess() {
		toastAndLog(sfcm.sfc.getContext(), TAG,
			String.format(String.valueOf(sfcm.sfc.getContext().getText(R.string.appDetail_downloadSuccess)), installedName),
			Toast.LENGTH_SHORT, Log.DEBUG);
		enableButtonRow(true);
		updateUI();
	}
	
	public void onDownloadError(Throwable t) {
		toastAndLog(sfcm.sfc.getContext(), TAG,
			String.format(String.valueOf(sfcm.sfc.getContext().getText(R.string.err_downloadFailed)), installedName), t,
			Toast.LENGTH_SHORT, Log.WARN);
		enableButtonRow(true);
		updateUI();
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
	
	public void registerAppDetailActivity(AppDetailActivity activity) {
		doSynchronized(() -> {
			myActivity = activity;
			updateActivity();
		});
	}
	
	public void unRegisterAppDetailActivity(@Nullable AppDetailActivity activity) {
		doSynchronized(() -> {
			if (activity == null || !activity.isFinishing())
				return;
			
			if (myActivity == activity)
				myActivity = null;
		});
	}
	
	public void registerListItem(AppOverviewFragment.AppOverviewListItem listItem) {
		myListItem = listItem;
		updateListItem();
	}
	
	public void updateListItem() {
		if (myListItem == null)
			return;
		
		runOnAppOverviewThread(() -> {
			myListItem.view.setOnClickListener(v -> AppDetailActivity.start(v.getContext(), id));
			myListItem.name.setText(installedName);
			myListItem.versionInstalled.setText(getVersionString(installedVersion));
			if (hasUpdates)
				myListItem.versionUpdate.setText(getVersionString(updateVersion));
			setVisibilityOf(myListItem.versionUpdate, hasUpdates);
			
			myListItem.icon.setImageDrawable(getIcon());
			setVisibilityOf(myListItem.placeholder, false);
		});
	}
	
	public void updateActivity() {
		runOnAppDetailThread((activity) -> {
			activity.deleteButton.setOnClickListener(v1 -> onDeleteButtonClick());
			activity.downloadButton.setOnClickListener(view1 -> onDownloadButtonClick());
			activity.installButton.setOnClickListener(view -> onInstallButtonClick());
			activity.updateButton.setOnClickListener(view -> onUpdateButtonClick());
			
			activity.nameView.setText(installedName);
			activity.urlView.setText(updateUrl.toString());
			activity.lastUpdatedView.setText(getLastUpdatedString());
			activity.installedVersionView.setText(installedVersion != null ? installedVersion.toString() : "not installed"); //TODO
			activity.updateVersionView.setText(updateVersion.toString());
			setVisibilityOf(activity.updateVersionContainer, hasUpdates);
			
			activity.icon.setImageDrawable(getIcon());
			activity.icon.setOnClickListener((b) -> {
				if (installedPackageName != null) {
					Context context = b.getContext();
					context.startActivity(context.getPackageManager().getLaunchIntentForPackage(installedPackageName));
				}
			});
			
			enableButtonRow(buttonRowEnabled);
			activity.installButton.setEnabled(buttonRowEnabled);
			activity.downloadButton.setEnabled(buttonRowEnabled);
			activity.deleteButton.setEnabled(buttonRowEnabled);
			activity.updateButton.setEnabled(buttonRowEnabled);
			setVisibilityOf(activity.updateProgressbar, updateProgressBarVisible);
			setVisibilityOf(activity.downloadProgressBar, downloadProgressBarVisible);
			setVisibilityOf(activity.installProgressBar, installProgressBarVisible);
			setVisibilityOf(activity.deleteProgressBar, deleteProgressbarVisible);
			
			setProgressBar(progressBarCurrent, progressBarMax);
			setVisibilityOf(activity.progressBar, progressBarVisible);
			
			updateDebugView();
		});
		new Thread(AppList::saveChanges).start();
	}
	
	boolean isInstalled() {
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
		if (isDownloadValid())
			try {
				AndroidApk apk = new AndroidApk(new File(cachePath));
				installedPackageName = apk.getPackageName();
			} catch (IOException ignore) {}
		return false;
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
	
	void delete() {
		deleteCacheFile();
		AppList.removeFromList(this);
	}
	
	private void doSynchronized(Runnable runnable) {
		lock.lock();
		try {
			runnable.run();
		} finally {
			lock.unlock();
		}
	}
	
	private void doSynchronizedOrAsync(Runnable runnable) {
		if (lock.tryLock())
			try {
				runnable.run();
			} finally {
				lock.unlock();
			}
		else
			new Thread(() -> doSynchronized(runnable)).start();
	}
	
	private void setVisibilityOf(View v, boolean visible) {
		runOnAppDetailThread((activity) -> {
			if (activity.findViewById(v.getId()) == v)
				v.setVisibility(visible ? VISIBLE : GONE);
		});
		runOnAppOverviewThread(() -> {
			if (sfcm.sfc.appOverviewFragment != null && myListItem != null) {
				if (myListItem.view != null && myListItem.view.findViewById(v.getId()) == v)
					v.setVisibility(visible ? VISIBLE : GONE);
			}
		});
	}
	
	private void install() {
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
		
		try {
			try {
				RootInstaller.install(cachePath, sfcm.sfc.getContext());
			} catch (RootInstaller.ShellOpenException e) {
				Installer.install(apkfile, sfcm.sfc.getContext());
			}
		} catch (InstallNotPermittedException | IOException e) {
			onDownloadError(e);
		} catch (RemoteException e) {
			onDownloadError(e.getSuppressed().length == 0 ? e : e.getSuppressed()[0]);
		}
	}
	
	private String getLastUpdatedString() {
		return getDateTimeStringForInstant(lastUpdated);
	}
	
	@RunInBackground
	private void download() {
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
	private Tupel<Version, String> getLatestVersionCode() throws IOException {
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
									lastVersion = new Tupel<>(version, toAbsolutePath(updateUrl, path)); // cache
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
	
	private boolean isDownloadValid() {
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
	
	private void deleteCacheFile() {
		if (cachePath == null || cachePath.isEmpty())
			return;
		File file = new File(cachePath);
		if (!file.exists() || file.delete()) {
			cachePath = "";
			cacheFileSize = 0;
		}
	}
	
	private void onInstallSuccess() {
		toastAndLog(sfcm.sfc.getContext(), TAG,
			String.format(String.valueOf(sfcm.sfc.getContext().getText(R.string.appDetail_installSuccess)), installedName),
			Toast.LENGTH_SHORT, Log.DEBUG);
		enableButtonRow(true);
		installedVersion = updateVersion;
		lastUpdated = System.currentTimeMillis();
		deleteCacheFile();
		isInstalled();
		hasUpdates = false;
		updateUI();
	}
	
	private void onInstallError(Throwable t) {
		toastAndLog(sfcm.sfc.getContext(), TAG,
			String.format(String.valueOf(sfcm.sfc.getContext().getText(R.string.err_installFailed)), installedName), t,
			Toast.LENGTH_LONG, Log.WARN);
		enableButtonRow(true);
		updateUI();
	}
	
	private void onInstallUpToDate() {
		toastAndLog(sfcm.sfc.getContext(), TAG,
			String.format(String.valueOf(sfcm.sfc.getContext().getText(R.string.appDetail_updateUpToDate)), installedName),
			Toast.LENGTH_SHORT, Log.DEBUG);
		enableButtonRow(true);
		updateUI();
	}
	
	@RunningInBackground
	private void onUpdateButtonClick() {
		enableButtonRow(false);
		updateProgressBarVisible = true;
		updateUI();
		
		new Thread(() -> {
			try {
				checkForUpdates();
				if (hasUpdates)
					toast(sfcm.sfc.getContext(), sfcm.sfc.getContext().getString(R.string.appDetail_updateAvailable),
						Toast.LENGTH_SHORT);
				else
					toast(sfcm.sfc.getContext(), sfcm.sfc.getContext().getString(R.string.appDetail_updateUpToDate), Toast.LENGTH_SHORT);
			} catch (IOException e) {
				toastAndLog(sfcm.sfc.getContext(), TAG, sfcm.sfc.getContext().getString(R.string.err_updateFailed), e,
					Toast.LENGTH_SHORT, Log.WARN);
			}
			enableButtonRow(true);
			updateUI();
		}).start();
	}
	
	private void runOnAppDetailThread(Consumer<AppDetailActivity> action) {
		doSynchronizedOrAsync(() -> {
			if (myActivity != null)
				myActivity.runOnUiThread(() -> doSynchronized(() -> {
					if (myActivity != null)
						action.accept(myActivity);
				}));
		});
	}
	
	private void onDeleteButtonClick() {
		enableButtonRow(false);
		deleteProgressbarVisible = true;
		updateUI();
		if (myActivity != null) {
			new AlertDialog.Builder(myActivity, R.style.AppTheme_Dialog)
				.setTitle(sfcm.sfc.getContext().getString(R.string.appDetail_alertDelete_header))
				.setMessage(sfcm.sfc.getContext().getString(R.string.appDetail_alertDelete_content))
				.setPositiveButton(android.R.string.yes, (dialog, which) -> {
					delete();
					doSynchronized(() -> unRegisterAppDetailActivity(myActivity));
					updateUI();
				})
				.setNegativeButton(android.R.string.no, (dialog, which) -> {
					enableButtonRow(true);
					updateUI();
				})
				.setOnCancelListener(dialog -> {
					enableButtonRow(true);
					updateUI();
				})
				.setIcon(android.R.drawable.ic_dialog_alert)
				.show();
		}
	}
	
	@RunningInBackground
	private void onDownloadButtonClick() {
		enableButtonRow(false);
		downloadProgressBarVisible = true;
		progressBarCurrent = 0L;
		progressBarMax = 0L;
		updateUI();
		
		new Thread(this::download).start();
	}
	
	private void onInstallButtonClick() {
		enableButtonRow(false);
		installProgressBarVisible = true;
		progressBarCurrent = 0;
		progressBarMax = 0;
		
		updateUI();
		new Thread(this::install).start();
	}
	
	private void enableButtonRow(boolean enabled) {
		buttonRowEnabled = enabled;
		updateProgressBarVisible = false;
		downloadProgressBarVisible = false;
		installProgressBarVisible = false;
		deleteProgressbarVisible = false;
		
		if (enabled) {
			progressBarCurrent = -1;
			progressBarMax = -1;
		}
	}
	
	private void setProgressBar(long progress, long max) {
		if (max < 0 || progress < 0) {
			progressBarVisible = false;
			runOnAppDetailThread((activity) -> setVisibilityOf(activity.progressBar, false));
		} else
			runOnAppDetailThread((activity) -> {
				if (max == 0 && progress == 0)
					activity.progressBar.setIndeterminate(true);
				else {
					activity.progressBar.setIndeterminate(false);
					int normalizedProgress;
					if (max > Integer.MAX_VALUE || progress > Integer.MAX_VALUE) {
						int steps = 1000;
						normalizedProgress = toProgress(progress, max, steps);
						activity.progressBar.setMax(steps);
					} else {
						normalizedProgress = (int) ((progress > max) ? max : progress);
						activity.progressBar.setMax((int) max);
					}
					
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
						activity.progressBar.setProgress(normalizedProgress, true);
					else
						activity.progressBar.setProgress(normalizedProgress);
				}
				progressBarVisible = true;
				setVisibilityOf(activity.progressBar, true);
			});
	}
	
	@SuppressWarnings("ConstantConditions")
	private void updateDebugView() {
		if (BuildConfig.APPLICATION_ID.endsWith(".dev"))
			runOnAppDetailThread((activity) -> activity.debugView.setText(prettyGson.toJson(this)));
		else
			runOnAppDetailThread((activity) -> setVisibilityOf(activity.debugView, false));
	}
	
	private void updateUI() {
		updateActivity();
		updateListItem();
	}
}
