package de.binarynoise.appdate;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import de.binarynoise.appdate.app.AppList;
import de.binarynoise.appdate.app.AppTemplate;
import de.binarynoise.appdate.ui.AddAppFragment;
import de.binarynoise.appdate.ui.AppOverviewFragment;
import de.binarynoise.appdate.ui.MainActivity;
import de.binarynoise.appdate.ui.PreferencesFragment;
import de.binarynoise.appdate.util.Util;

import static de.binarynoise.appdate.ui.AddAppFragment.appFilterPattern;

public class SFC {
	public static final SFC                   sfcm = new SFC();
	public final        StaticFieldsContainer sfc  = new StaticFieldsContainer();
	
	@SuppressWarnings("MethodMayBeStatic")
	public static class StaticFieldsContainer {
		private static final int                  JOB_ID         = 123456789;
		private static final String               TAG            = "SFC";
		@Nullable public     FloatingActionButton floatingActionButton;
		@Nullable public     AddAppFragment       addAppFragment;
		@Nullable public     AppOverviewFragment  appOverviewFragment;
		@Nullable public     PreferencesFragment  preferencesFragment;
		@Nullable public     MainActivity         mainActivity;
		private              Context              context;
		private volatile     boolean              shallInitalize = true;
		
		public void initalizeIfNotYetInitalized(Context applicationContext) {
			if (shallInitalize) {
				context = applicationContext;
				AppList.load();
				
				new Thread(() -> {
					checkPermissions();
					
					// 1.5 secs delay to speed up the start of the launched activity
					try {
						Thread.sleep(1500);
					} catch (InterruptedException ignored) {}
					scheduleBackgroundUpdate();
					
					loadTemplates();
					
					preloadInstalledAppLabels();
				}).start();
				
				shallInitalize = false;
			}
		}
		
		@NonNull
		public Context getContext() {
			if (context == null)
				throw new IllegalStateException("There should always be a context set");
			return context;
		}
		
		public void setContext(Context context) {
			this.context = context;
		}
		
		private void scheduleBackgroundUpdate() {
			JobInfo jobInfo = new JobInfo.Builder(JOB_ID, new ComponentName(getContext(), UpdateSchedulerService.class))
				.setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
				.setPeriodic(TimeUnit.HOURS.toMillis(4))
				.setBackoffCriteria(JobInfo.MAX_BACKOFF_DELAY_MILLIS, JobInfo.BACKOFF_POLICY_EXPONENTIAL)
				.setPersisted(false)
				.build();
			JobScheduler jobScheduler = (JobScheduler) (context.getSystemService(Context.JOB_SCHEDULER_SERVICE));
			if (jobScheduler != null)
				jobScheduler.schedule(jobInfo);
		}
		
		private void loadTemplates() {
			try {
				for (AppTemplate t : AppTemplate.getAvailableAppTemplates().values())
					AppList.addToList(t);
			} catch (IOException e) {
				Util.log(TAG, "fetching templates failed", e, Log.WARN);
			}
		}
		
		private void checkPermissions() {
			PackageManager packageManager = context.getPackageManager();
			List<PackageInfo> installedPackages = packageManager.getInstalledPackages(0);
			List<PackageInfo> userPackages = new ArrayList<>();
			
			for (PackageInfo installedPackage : installedPackages)
				if (!appFilterPattern.matcher(installedPackage.packageName).matches())
					userPackages.add(installedPackage);
			
			if (userPackages.size() <= 1) {
				Util.toastAndLog(sfcm.sfc.getContext(), TAG, "Please grant permission to read installed packages", Toast.LENGTH_LONG,
					Log.WARN);
				
				// most likely the permission isn't "granted" by XPrivacyLua, so we'll try to open it
				// that the user can revoke the restriction
				Intent intent = new Intent(Intent.ACTION_MAIN);
				intent.setComponent(ComponentName.unflattenFromString("eu.faircode.xlua/.ActivityMain"));
				intent.addCategory(Intent.CATEGORY_LAUNCHER);
				intent.putExtra("package", context.getPackageName());
				context.startActivity(intent);
			}
		}
		
		private void preloadInstalledAppLabels() {
			PackageManager pm = context.getPackageManager();
			List<PackageInfo> installedPackages = pm.getInstalledPackages(0);
			for (PackageInfo packageInfo : installedPackages)
				AddAppFragment.getAppName(pm, packageInfo);
		}
	}
}
