package de.binarynoise.appdate;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.util.Log;

import java.io.IOException;

import de.binarynoise.appdate.receiver.NotificationCallbackReceiver;
import de.binarynoise.appdate.util.RunInBackground;
import de.binarynoise.appdate.util.RunningInBackground;

import static de.binarynoise.appdate.SFC.sfcm;
import static de.binarynoise.appdate.receiver.NotificationCallbackReceiver.ACTION_OPEN;
import static de.binarynoise.appdate.receiver.NotificationCallbackReceiver.EXTRA_APP_PACKAGENAME;
import static de.binarynoise.appdate.util.Util.log;
import static de.binarynoise.appdate.util.Util.notification;

public class UpdateSchedulerService extends JobService {
	private static final String        TAG = "UpdateSchedulerService";
	private              Thread        thread;
	private              JobParameters jobParameters;
	
	@RunningInBackground
	@Override
	public void onCreate() {
		super.onCreate();
		sfcm.sfc.backgroundService = this;
		thread = new Thread(this::checkForUpdates);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		sfcm.sfc.backgroundService = null;
	}
	
	@Override
	public boolean onStartJob(JobParameters params) {
		sfcm.sfc.initalizeIfNotYetInitalized(getApplicationContext());
		jobParameters = params;
		thread.start();
		return true;
	}
	
	@Override
	public boolean onStopJob(JobParameters params) {
		thread.interrupt();
		return false;
	}
	
	@SuppressWarnings("ObjectAllocationInLoop")
	@RunInBackground
	private void checkForUpdates() {
		for (int i = 0; i < sfcm.sfc.appList.size(); i++) {
			App app = sfcm.sfc.appList.get(i);
			try {
				app.checkForUpdates();
			} catch (IOException e) {
				log(TAG, String.format("Could not check app '%s' for updates: ", app.installedName), e, Log.WARN);
			}
			if (app.hasUpdates) {
				Intent action = new Intent(this, NotificationCallbackReceiver.class);
				action.setAction(ACTION_OPEN);
				if (app.installedPackageName == null)
					action.putExtra(EXTRA_APP_PACKAGENAME, app.installedName);
				else
					action.putExtra(EXTRA_APP_PACKAGENAME, app.installedPackageName);
				
				String title = String.format("Update for %s", app.installedName);
				
				String text;
				if (app.isInstalled())
					text = String.format(
						"Appdate has found an update for app %s.\nThe currently installed Version is %s\nThe available version is %s",
						app.installedName, app.installedVersion, app.updateVersion);
				else
					text =
						String.format("Appdate can install app %s.\nThe available version is %s", app.installedName, app.updateVersion);
				Drawable drawable = app.getIcon();
				
				notification(drawable, title, text, action);
			}
		}
		sfcm.sfc.appList.saveChanges();
		jobFinished(jobParameters, true);
	}
}
