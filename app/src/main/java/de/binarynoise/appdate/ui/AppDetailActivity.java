package de.binarynoise.appdate.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import de.binarynoise.appdate.R;
import de.binarynoise.appdate.app.App;
import de.binarynoise.appdate.app.AppList;

import static de.binarynoise.appdate.SFC.sfcm;
import static de.binarynoise.appdate.util.Util.dumpBundle;

/**
 * The App detail activity.
 */
@SuppressWarnings({"FieldCanBeLocal", "WeakerAccess"})
public class AppDetailActivity extends AppCompatActivity {
	public static final String            EXTRA_APP     = "EXTRA_APP";
	public static final String            TAG           = "AppDetailActivity";
	public              ImageButton       deleteButton;
	public              ImageButton       downloadButton;
	public              ImageButton       installButton;
	public              ImageButton       updateButton;
	public              ProgressBar       deleteProgressBar;
	public              ProgressBar       downloadProgressBar;
	public              ProgressBar       installProgressBar;
	public              ProgressBar       updateProgressbar;
	public              ProgressBar       progressBar;
	public              TextInputEditText nameView;
	public              TextInputEditText urlView;
	public              TextInputEditText lastUpdatedView;
	public              TextInputEditText installedVersionView;
	public              TextInputEditText updateVersionView;
	public              TextInputLayout   updateVersionContainer;
	public              TextView          debugView;
	public              ImageView         icon;
	public              Thread            uiThread;
	private             App               app;
	@SuppressWarnings("RedundantFieldInitialization") //
	private volatile    boolean           hasRegistered = false;
	
	public static void start(Context context, @Nullable App app) {
		if (app != null)
			start(context, app.id);
	}
	
	@Deprecated
	public static void start(Context context, String packageName) {
		if (packageName != null && !packageName.isEmpty())
			start(context, AppList.findByPackageName(packageName));
	}
	
	public static void start(Context context, int id) {
		if (id != 0) {
			Intent starter = new Intent(context, AppDetailActivity.class);
			starter.putExtra(EXTRA_APP, id);
			context.startActivity(starter);
		}
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if (getIntent() == null || getIntent().getExtras() == null) {
			finish();
			return;
		}
		
		if (!getIntent().getExtras().containsKey(EXTRA_APP)) {
			dumpBundle(TAG, getIntent().getExtras());
			finish();
			return;
		}
		
		sfcm.sfc.initalizeIfNotYetInitalized(getApplicationContext());
		
		int id = getIntent().getIntExtra(EXTRA_APP, 0);
		//noinspection ConstantConditions
		app = AppList.findById(id);
		
		if (app == null) {
			finish();
			return;
		}
		
		setContentView(R.layout.activity_app_detail);
		
		Toolbar toolbar = findViewById(R.id.appDetail_toolbar);
		setSupportActionBar(toolbar);
		ActionBar supportActionBar = getSupportActionBar();
		if (supportActionBar != null && sfcm.sfc.mainActivity != null) {
			supportActionBar.setDisplayHomeAsUpEnabled(true);
			supportActionBar.setHomeButtonEnabled(true);
		}
		
		deleteButton = findViewById(R.id.appDetail_deleteButton);
		deleteProgressBar = findViewById(R.id.appDetail_deleteButton_progressBar);
		downloadButton = findViewById(R.id.appDetail_downloadButton);
		downloadProgressBar = findViewById(R.id.appDetail_downloadButton_progressBar);
		installButton = findViewById(R.id.appDetail_installButton);
		installProgressBar = findViewById(R.id.appDetail_installButton_progressBar);
		updateButton = findViewById(R.id.appDetail_updateButton);
		updateProgressbar = findViewById(R.id.appDetail_updateButton_progressBar);
		
		progressBar = findViewById(R.id.appDetail_progressBar);
		
		nameView = findViewById(R.id.appDetail_name);
		icon = findViewById(R.id.appDetail_icon);
		urlView = findViewById(R.id.appDetail_url);
		lastUpdatedView = findViewById(R.id.appDetail_last_updated);
		installedVersionView = findViewById(R.id.appDetail_installedVersion);
		updateVersionView = findViewById(R.id.appDetail_updateVersion);
		updateVersionContainer = findViewById(R.id.appDetail_updateVersion_float);
		
		debugView = findViewById(R.id.appDetail_textView);
		
		runOnUiThread(() -> {
			uiThread = Thread.currentThread();
			if (!hasRegistered) {
				app.registerAppDetailActivity(this);
				hasRegistered = true;
			}
		});
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		if (hasRegistered)
			app.updateActivity();
		else {
			app.registerAppDetailActivity(this);
			hasRegistered = true;
		}
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (isFinishing())
			app.unRegisterAppDetailActivity(this);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		AppList.saveChanges();
	}
}
