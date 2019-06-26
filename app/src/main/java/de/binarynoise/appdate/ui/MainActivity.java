package de.binarynoise.appdate.ui;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import de.binarynoise.appdate.R;
import de.binarynoise.appdate.UpdateSchedulerService;
import de.binarynoise.appdate.app.AppList;
import de.binarynoise.appdate.util.Util;

import static de.binarynoise.appdate.SFC.sfcm;
import static de.binarynoise.appdate.ui.AddAppFragment.appFilterPattern;

/**
 * The Main activity.
 */
public class MainActivity extends AppCompatActivity {
	private static final int    JOB_ID = 123456789;
	private static final String TAG    = "MainActivity";
	ViewPager viewPager;
	
	private static void scheduleBackgroundUpdate() {
		Context context = sfcm.sfc.getContext();
		JobInfo jobInfo = new JobInfo.Builder(JOB_ID, new ComponentName(context, UpdateSchedulerService.class))
			.setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
			.setPeriodic(TimeUnit.HOURS.toMillis(4))
			.setBackoffCriteria(JobInfo.MAX_BACKOFF_DELAY_MILLIS, JobInfo.BACKOFF_POLICY_EXPONENTIAL)
			.setPersisted(false)
			.build();
		JobScheduler jobScheduler = (JobScheduler) (context.getSystemService(Context.JOB_SCHEDULER_SERVICE));
		if (jobScheduler != null)
			jobScheduler.schedule(jobInfo);
	}
	
	private static void checkPermissions() {
		Context context = sfcm.sfc.getContext();
		PackageManager packageManager = context.getPackageManager();
		List<PackageInfo> installedPackages = packageManager.getInstalledPackages(0);
		List<PackageInfo> userPackages = new ArrayList<>();
		
		for (PackageInfo installedPackage : installedPackages)
			if (!appFilterPattern.matcher(installedPackage.packageName).matches())
				userPackages.add(installedPackage);
		
		if (userPackages.size() <= 1) {
			Util.toastAndLog(context, TAG, "Please grant permission to read installed packages", Toast.LENGTH_LONG, Log.WARN);
			
			// most likely the permission isn't "granted" by XPrivacyLua, so we'll try to open it
			// that the user can revoke the restriction
			Intent intent = new Intent(Intent.ACTION_MAIN);
			intent.setComponent(ComponentName.unflattenFromString("eu.faircode.xlua/.ActivityMain"));
			intent.addCategory(Intent.CATEGORY_LAUNCHER);
			intent.putExtra("package", context.getPackageName());
			context.startActivity(intent);
		}
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		sfcm.sfc.initalizeIfNotYetInitalized(getApplicationContext());
		setContentView(R.layout.activity_main);
		
		Toolbar toolbar = findViewById(R.id.mainActivity_toolbar);
		setSupportActionBar(toolbar);
		if (getPackageName().endsWith(".dev"))
			toolbar.setTitle(toolbar.getTitle() + ".dev");
		
		FloatingActionButton floatingActionButton = findViewById(R.id.fab);
		if (floatingActionButton != null)
			sfcm.sfc.floatingActionButton = floatingActionButton;
		if (sfcm.sfc.floatingActionButton == null)
			throw new RuntimeException("FloatingActionButton not available, although it should be",
				new NullPointerException("FloatingActionButton is null"));
		sfcm.sfc.floatingActionButton.setOnClickListener(v -> viewPager.setCurrentItem(0));
		
		// Create the adapter that will return a fragment for each of the three
		// primary sections of the activity.
		// The PagerAdapter that will provide fragments for each of the sections.
		// Set up the ViewPager with the sections adapter.
		viewPager = findViewById(R.id.container);
		viewPager.setAdapter(new SectionsPagerAdapter(getSupportFragmentManager()));
		
		TabLayout tabLayout = findViewById(R.id.tabs);
		
		viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
		tabLayout.addOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(viewPager));
		viewPager.setCurrentItem(1);
	}
	
	@Override
	protected void onPostCreate(@Nullable Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		checkPermissions();
		scheduleBackgroundUpdate();
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		sfcm.sfc.floatingActionButton = findViewById(R.id.fab);
		sfcm.sfc.mainActivity = this;
		sfcm.sfc.initalizeIfNotYetInitalized(getApplicationContext());
	}
	
	@Override
	public void onStop() {
		super.onStop();
		if (sfcm.sfc.mainActivity == this)
			sfcm.sfc.mainActivity = null;
		
		if (sfcm.sfc.floatingActionButton == findViewById(R.id.fab))
			sfcm.sfc.floatingActionButton = null;
	}
	
	@Override
	protected void onPause() {
		AppList.saveChanges();
		super.onPause();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		AppList.sortListAndUpdate();
	}
	
	/**
	 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
	 * one of the sections/tabs/pages.
	 */
	static class SectionsPagerAdapter extends FragmentPagerAdapter {
		/**
		 * Instantiates a new Sections pager adapter.
		 *
		 * @param fm the fm
		 */
		SectionsPagerAdapter(FragmentManager fm) {
			super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
		}
		
		@NonNull
		@Override
		public Fragment getItem(int position) {
			switch (position) {
				case 0:
					if (sfcm.sfc.addAppFragment == null)
						sfcm.sfc.addAppFragment = new AddAppFragment();
					return sfcm.sfc.addAppFragment;
				case 1:
					if (sfcm.sfc.appOverviewFragment == null)
						sfcm.sfc.appOverviewFragment = new AppOverviewFragment();
					return sfcm.sfc.appOverviewFragment;
				case 2:
					if (sfcm.sfc.preferencesFragment == null)
						sfcm.sfc.preferencesFragment = new PreferencesFragment();
					return sfcm.sfc.preferencesFragment;
				default:
					throw new RuntimeException(String.format("Invalid fragment count: %d", position));
			}
		}
		
		@Override
		public int getCount() { return 3; }
	}
}
