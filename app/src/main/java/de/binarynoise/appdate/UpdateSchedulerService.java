package de.binarynoise.appdate;

import android.app.job.JobParameters;
import android.app.job.JobService;

import de.binarynoise.appdate.app.AppList;
import de.binarynoise.appdate.util.RunInBackground;
import de.binarynoise.appdate.util.RunningInBackground;

import static de.binarynoise.appdate.SFC.sfcm;

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
	
	@RunInBackground
	private void checkForUpdates() {
		AppList.checkForUpdates();
		jobFinished(jobParameters, true);
	}
}
