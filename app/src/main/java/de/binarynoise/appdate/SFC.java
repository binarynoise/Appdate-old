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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import de.binarynoise.appdate.app.AppList;
import de.binarynoise.appdate.ui.AddAppFragment;
import de.binarynoise.appdate.ui.AppOverviewFragment;
import de.binarynoise.appdate.ui.MainActivity;
import de.binarynoise.appdate.ui.PreferencesFragment;
import de.binarynoise.appdate.util.Util;

import static de.binarynoise.appdate.ui.AddAppFragment.appFilterPattern;

@SuppressWarnings({"WeakerAccess", "unused"})

public class SFC {
	public static final SFC                   sfcm = new SFC();
	public final        StaticFieldsContainer sfc  = new StaticFieldsContainer();
	
	public static class StaticFieldsContainer {
		private static final int                    JOB_ID         = 123456789;
		private static final String                 TAG            = "SFC";
		public final         AppList                appList        = new AppList();
		@Nullable public     FloatingActionButton   floatingActionButton;
		@Nullable public     AddAppFragment         addAppFragment;
		@Nullable public     AppOverviewFragment    appOverviewFragment;
		@Nullable public     PreferencesFragment    preferencesFragment;
		@Nullable public     MainActivity           mainActivity;
		@Nullable public     UpdateSchedulerService backgroundService;
		@Nullable public     DownloadManager        downloadManager;
		private              Context                context;
		private volatile     boolean                shallInitalize = true;
		
		public void initalizeIfNotYetInitalized(Context applicationContext) {
			if (shallInitalize) {
				context = applicationContext;
				appList.load();
				
				new Thread(() -> {
					JobInfo jobInfo =
						new JobInfo.Builder(JOB_ID, new ComponentName(sfcm.sfc.getContext(), UpdateSchedulerService.class))
							.setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
							.setPeriodic(TimeUnit.HOURS.toMillis(4))
							.setBackoffCriteria(JobInfo.MAX_BACKOFF_DELAY_MILLIS, JobInfo.BACKOFF_POLICY_EXPONENTIAL)
							.setPersisted(false)
							.build();
					JobScheduler jobScheduler = (JobScheduler) (context.getSystemService(Context.JOB_SCHEDULER_SERVICE));
					if (jobScheduler != null)
						jobScheduler.schedule(jobInfo);
					
					getContext().startService(new Intent(context, DownloadManagerService.class));
					
					checkPermissions();
				}).start();
				
				Util.log(TAG, "init completed", Log.DEBUG);
				shallInitalize = false;
			}
		}
		
		public void checkPermissions() {
			PackageManager packageManager = context.getPackageManager();
			List<PackageInfo> installedPackages = packageManager.getInstalledPackages(0);
			List<PackageInfo> userPackages = new ArrayList<>();
			
			for (PackageInfo installedPackage : installedPackages)
				if (!appFilterPattern.matcher(installedPackage.packageName).matches())
					userPackages.add(installedPackage);
			
			if (userPackages.size() == 1)
				Util.toast(sfcm.sfc.getContext(), "Please grant permission to read installed packages", Toast.LENGTH_LONG);
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
		
		boolean isContextSet() {
			return context == null;
		}
	}
}
