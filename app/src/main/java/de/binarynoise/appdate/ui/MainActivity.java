package de.binarynoise.appdate.ui;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import de.binarynoise.appdate.R;
import de.binarynoise.appdate.app.AppList;

import static de.binarynoise.appdate.SFC.sfcm;

/**
 * The Main activity.
 */
public class MainActivity extends AppCompatActivity {
	private static final String TAG = "MainActivity";
	ViewPager viewPager;
	
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
			super(fm);
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
