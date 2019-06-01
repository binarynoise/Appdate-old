package de.binarynoise.appdate;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.graphics.drawable.Drawable;

import java.io.IOException;

import de.binarynoise.appdate.app.App;
import de.binarynoise.appdate.app.AppList;
import de.binarynoise.appdate.receiver.NotificationCallbackReceiver;
import de.binarynoise.appdate.util.RunInBackground;
import de.binarynoise.appdate.util.RunningInBackground;

import static de.binarynoise.appdate.SFC.sfcm;
import static de.binarynoise.appdate.receiver.NotificationCallbackReceiver.ACTION_OPEN;
import static de.binarynoise.appdate.receiver.NotificationCallbackReceiver.EXTRA_APP_ID;
import static de.binarynoise.appdate.util.Util.notification;

public class UpdateSchedulerService extends JobService {
	private static final String        TAG = "UpdateSchedulerService";
	private              Thread        thread;
	private              JobParameters jobParameters;
	
	@RunningInBackground
	@Override
	public boolean onStartJob(JobParameters params) {
		sfcm.sfc.initalizeIfNotYetInitalized(getApplicationContext());
		jobParameters = params;
		thread = new Thread(this::checkForUpdates);
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
		App[] all = AppList.getAll();
		for (App app : all) {
			try {
				app.checkForUpdates();
			} catch (IOException ignored) {}
			if (app.hasUpdates) {
				Intent action = new Intent(this, NotificationCallbackReceiver.class);
				action.setAction(ACTION_OPEN);
				action.putExtra(EXTRA_APP_ID, app.id);
				
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
				
				int id = app.id;
				notification(id, drawable, title, text, action);
			}
		}
		AppList.saveChanges();
		jobFinished(jobParameters, true);
	}
}
