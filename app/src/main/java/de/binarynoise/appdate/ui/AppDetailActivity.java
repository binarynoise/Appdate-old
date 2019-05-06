package de.binarynoise.appdate.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.io.IOException;

import de.binarynoise.appdate.App;
import de.binarynoise.appdate.R;
import de.binarynoise.appdate.util.RunningInBackground;
import de.binarynoise.appdate.util.TextChangedListener;

import static android.view.View.*;
import static de.binarynoise.appdate.SFC.sfcm;
import static de.binarynoise.appdate.util.Util.*;

/**
 * The App detail activity.
 */
@SuppressWarnings("FieldCanBeLocal")
public class AppDetailActivity extends AppCompatActivity {
	private static final String      EXTRA_APP = AppDetailActivity.class.getName() + ".EXTRA_APP";
	private static final String      TAG       = "AppDetailActivity";
	private              App         app;
	private              TextView    nameView;
	private              TextView    urlView;
	private              TextView    lastUpdatedView;
	private              TextView    debugView;
	private              ImageButton deleteButton;
	private              ImageButton downloadButton;
	private              ImageButton installButton;
	private              ImageButton updateButton;
	private              ProgressBar deleteProgressBar;
	private              ProgressBar downloadProgressBar;
	private              ProgressBar installProgressBar;
	private              ProgressBar updateButtonProgressbar;
	private              Button      applyButton;
	private              Button      cancelButton;
	private              ProgressBar progressBar;
	
	public static void start(Context context, @Nullable App app) {
		if (app != null)
			start(context, sfcm.sfc.appList.getIndex(app));
	}
	
	public static void start(Context context, int id) {
		if (id >= 0 && id < sfcm.sfc.appList.size()) {
			Intent starter = new Intent(context, AppDetailActivity.class);
			starter.putExtra(EXTRA_APP, id);
			context.startActivity(starter);
		}
	}
	
	public static void start(Context context, String packageName) {
		if (packageName != null && !packageName.isEmpty())
			start(context, sfcm.sfc.appList.find(packageName));
	}
	
	@SuppressWarnings("MethodWithMultipleReturnPoints")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		if (getIntent() == null || getIntent().getExtras() == null) {
			finish();
			return;
		}
		
		if (!getIntent().getExtras().containsKey(EXTRA_APP)) {
			dumpBundle(TAG, getIntent().getExtras());
			finish();
			return;
		}
		
		int id = (int) getIntent().getSerializableExtra(EXTRA_APP);
		app = sfcm.sfc.appList.get(id);
		
		if (app == null) {
			finish();
			return;
		}
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_app_detail);
		
		Toolbar toolbar = findViewById(R.id.appDetail_toolbar);
		setSupportActionBar(toolbar);
		ActionBar supportActionBar = getSupportActionBar();
		if (supportActionBar != null) {
			supportActionBar.setDisplayHomeAsUpEnabled(true);
			supportActionBar.setHomeButtonEnabled(true);
		}
		
		nameView = findViewById(R.id.appDetail_name);
		nameView.setText(app.installedName);
		nameView.addTextChangedListener(new TextChangedListener() {
			@Override
			public void onTextChange(String s) {
				applyButton.setEnabled(!s.isEmpty());
			}
		});
		
		urlView = findViewById(R.id.appDetail_url);
		urlView.setText((app.updateUrl) == null ? "" : app.updateUrl.toString());
		urlView.addTextChangedListener(new TextChangedListener() {
			@Override
			public void onTextChange(String s) {
				applyButton.setEnabled(!s.isEmpty());
			}
		});
		
		lastUpdatedView = findViewById(R.id.appDetail_last_updated);
		lastUpdatedView.setText(app.getLastUpdatedString());
		
		deleteButton = findViewById(R.id.appDetail_deleteButton);
		deleteButton.setOnClickListener(v1 -> onDeleteButtonClick());
		deleteProgressBar = findViewById(R.id.appDetail_deleteButton_progressBar);
		
		downloadButton = findViewById(R.id.appDetail_downloadButton);
		downloadButton.setOnClickListener(view1 -> onDownloadButtonClick());
		downloadProgressBar = findViewById(R.id.appDetail_downloadButton_progressBar);
		
		installButton = findViewById(R.id.appDetail_installButton);
		installButton.setOnClickListener(view -> onInstallButtonClick());
		installProgressBar = findViewById(R.id.appDetail_installButton_progressBar);
		
		updateButton = findViewById(R.id.appDetail_updateButton);
		updateButton.setOnClickListener(view -> onUpdateButtonClick());
		
		progressBar = findViewById(R.id.appDetail_progressBar);
		setProgressBar(-1, -1);
		
		debugView = findViewById(R.id.appDetail_textView);
		updateDebugView();
		
		applyButton = findViewById(R.id.appDetail_applyButton);
		applyButton.setOnClickListener(view -> onApplyButtonClick());
		
		cancelButton = findViewById(R.id.appDetail_cancelButton);
		cancelButton.setOnClickListener(v -> onBackPressed());
		
		setupCallbacks();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		sfcm.sfc.appList.saveChanges();
	}
	
	@RunningInBackground
	private void onUpdateButtonClick() {
		enableButtonRow(false);
		updateButtonProgressbar.setVisibility(VISIBLE);
		new Thread(() -> {
			try {
				app.checkForUpdates();
				if (app.hasUpdates)
					toast(this, getString(R.string.appDetail_updateAvailable), Toast.LENGTH_SHORT);
				else
					toast(this, getString(R.string.appDetail_updateUpToDate), Toast.LENGTH_SHORT);
			} catch (IOException e) {
				toastAndLog(this, TAG, getString(R.string.appDetail_updateFailed), e, Toast.LENGTH_SHORT, Log.WARN);
			}
			runOnUiThread(() -> enableButtonRow(true));
		}).start();
	}
	
	private void setupCallbacks() {
		app.downloadSuccessCallback = () -> {
			toastAndLog(this, TAG, String.format(String.valueOf(getText(R.string.appDetail_downloadSuccess)), app.installedName),
				Toast.LENGTH_SHORT, Log.DEBUG);
			enableButtonRow(true);
		};
		
		app.downloadErrorCallback = e -> {
			Log.w(TAG, e);
			toastAndLog(this, TAG, String.format(String.valueOf(getText(R.string.appDetail_downloadFailed)), app.installedName), e,
				Toast.LENGTH_SHORT, Log.WARN);
			enableButtonRow(true);
		};
		
		app.downloadProgressCallback = this::setProgressBar;
		
		app.installSuccessCallback = () -> {
			toastAndLog(this, TAG, String.format(String.valueOf(getText(R.string.appDetail_installSuccess)), app.installedName),
				Toast.LENGTH_SHORT, Log.DEBUG);
			enableButtonRow(true);
		};
		
		app.installUpToDateCallback = () -> {
			toastAndLog(this, TAG, String.format(String.valueOf(getText(R.string.appDetail_updateUpToDate)), app.installedName),
				Toast.LENGTH_SHORT, Log.DEBUG);
			enableButtonRow(true);
		};
		
		app.installErrorCallback = e -> {
			toastAndLog(this, TAG, String.format(String.valueOf(getText(R.string.appDetail_installFailed)), app.installedName), e,
				Toast.LENGTH_LONG, Log.WARN);
			enableButtonRow(true);
		};
	}
	
	private void onDeleteButtonClick() {
		enableButtonRow(false);
		deleteProgressBar.setVisibility(INVISIBLE);
		new AlertDialog.Builder(this)
			.setTitle(getString(R.string.appDetail_alertDelete_header))
			.setMessage(getString(R.string.appDetail_alertDelete_content))
			.setPositiveButton(android.R.string.yes, (dialog, which) -> {
				sfcm.sfc.appList.removeFromList(app);
				finish();
			})
			.setNegativeButton(android.R.string.no, (dialog, which) -> enableButtonRow(true))
			.setOnCancelListener(dialog -> enableButtonRow(true))
//			.setNeutralButton("Cache file only", (dialog, which) -> app.deleteCacheFile())
			.setIcon(android.R.drawable.ic_dialog_alert)
			.show();
	}
	
	@RunningInBackground
	private void onDownloadButtonClick() {
		enableButtonRow(false);
		downloadProgressBar.setVisibility(VISIBLE);
		setProgressBar(0, 0);
		
		new Thread(() -> app.download()).start();
	}
	
	private void onInstallButtonClick() {
		enableButtonRow(false);
		installProgressBar.setVisibility(VISIBLE);
		setProgressBar(0, 0);
		new Thread(() -> app.install()).start();
	}
	
	private void onApplyButtonClick() {
		new Thread(() -> {
			app.setInstalledName(nameView.getText().toString());
			runOnUiThread(() -> {
				updateDebugView();
				applyButton.setEnabled(false);
				sfcm.sfc.appList.saveChanges();
			});
		}).start();
	}
	
	private void updateDebugView() {
		runOnUiThread(() -> debugView.setText(prettyGson.toJson(app)));
	}
	
	private void enableButtonRow(boolean enabled) {
		runOnUiThread(() -> {
			installButton.setEnabled(enabled);
			installProgressBar.setVisibility(INVISIBLE);
			downloadButton.setEnabled(enabled);
			downloadProgressBar.setVisibility(INVISIBLE);
			deleteButton.setEnabled(enabled);
			deleteProgressBar.setVisibility(INVISIBLE);
			updateButton.setEnabled(enabled);
			updateButtonProgressbar.setVisibility(INVISIBLE);
			
			if (enabled)
				setProgressBar(-1, -1);
			updateDebugView();
		});
	}
	
	private void setProgressBar(long progress, long max) {
		runOnUiThread(() -> {
			if (max < 0 || progress < 0)
				progressBar.setVisibility(GONE);
			else {
				progressBar.setVisibility(VISIBLE);
				if (max == 0 && progress == 0)
					progressBar.setIndeterminate(true);
				else {
					progressBar.setIndeterminate(false);
					int normalizedProgress;
					if (max > Integer.MAX_VALUE || progress > Integer.MAX_VALUE) {
						int steps = 1000;
						normalizedProgress = toProgress(progress, max, steps);
						progressBar.setMax(steps);
					} else {
						normalizedProgress = (int) ((progress > max) ? max : progress);
						progressBar.setMax((int) max);
					}
					
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
						progressBar.setProgress(normalizedProgress, true);
					else
						progressBar.setProgress(normalizedProgress);
				}
			}
		});
	}
}
