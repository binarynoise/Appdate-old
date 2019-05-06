package de.binarynoise.appdate.ui;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.regex.Pattern;

import de.binarynoise.appdate.App;
import de.binarynoise.appdate.R;
import de.binarynoise.appdate.util.RunningInBackground;
import de.binarynoise.appdate.util.TextChangedListener;
import de.binarynoise.appdate.util.Util;

import static de.binarynoise.appdate.SFC.sfcm;
import static de.binarynoise.appdate.util.Util.toast;
import static de.binarynoise.appdate.util.Util.toastAndLog;

/**
 * A simple {@link Fragment} subclass.
 */
public class AddAppFragment extends Fragment {
	public static final  Pattern          appFilterPattern =
		Pattern.compile("^((com\\.|org\\.)?(google|android|cyanogenmod|lineage)).*$");
	private static final Pattern          urlFilterPattern = Pattern.compile("\\s");
	private static final String           TAG              = "AddApp";
	private static final String           PACKAGE_NAME     = "packageName";
	private static final String           PACKAGE_INFO     = "packageInfo";
	@Nullable
	private              App              tempApp;
	private              PackageInfo      tempInfo;
	private              View             myView;
	private              EditText         nameView;
	private              EditText         urlView;
	private              Spinner          packagenamespinner;
	private              TextView         packageName;
	private              Button           addButton;
	private              Button           testButton;
	private              CheckBox         installedCheckBox;
	private              ConstraintLayout packageDetailContainer;
	private              ProgressBar      testButtonProgressBar;
	
	@Override
	public void setUserVisibleHint(boolean isVisibleToUser) {
		super.setUserVisibleHint(isVisibleToUser);
		
		if (isVisibleToUser) {
			//hide fab
			if (sfcm.sfc.floatingActionButton != null) {
				sfcm.sfc.floatingActionButton.animate().alpha(0.0F);
				sfcm.sfc.floatingActionButton.setClickable(false);
			}
			
			//show keyboard if text input fields are empty
			if (urlView.getText().length() == 0) {
				urlView.setFocusableInTouchMode(true);
				urlView.requestFocus();
				
				if (getActivity() != null) {
					getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
					InputMethodManager systemService =
						(InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
					if (systemService != null)
						systemService.showSoftInput(urlView, 0);
				}
			}
		} else { // not visible to user
			//show fab
			if (sfcm.sfc.floatingActionButton != null) {
				sfcm.sfc.floatingActionButton.animate().alpha(1.0F);
				sfcm.sfc.floatingActionButton.setClickable(true);
			}
			
			//show keyboard
			FragmentActivity activity = getActivity();
			if (activity != null) {
				activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
				InputMethodManager systemService = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
				if (systemService != null)
					systemService.hideSoftInputFromWindow(myView.getWindowToken(), 0);
			}
		}
	}
	
	/**
	 * Inflate the layout for this fragment
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		myView = inflater.inflate(R.layout.fragment_add_app, container, false);
		
		testButton = myView.findViewById(R.id.addApp_testButton);
		testButton.setOnClickListener(v -> onTestButtonClick());
		
		addButton = myView.findViewById(R.id.addApp_addButton);
		addButton.setOnClickListener(this::onAddButtonClick);
		
		testButtonProgressBar = myView.findViewById(R.id.addApp_testButton_progressBar);
		
		nameView = myView.findViewById(R.id.addApp_name);
		nameView.addTextChangedListener(new TextChangedListener() {
			@Override
			public void onTextChange(String s) {
				if (tempApp != null) {
					String installedName = tempApp.setInstalledName(s);
					if (!installedName.equalsIgnoreCase(s))
						nameView.setText(installedName);
				}
			}
		});
		
		urlView = myView.findViewById(R.id.addApp_url);
		urlView.addTextChangedListener(new TextChangedListener() {
			@Override
			public void onTextChange(String s) {
				addButton.setEnabled(false);
			}
		});
		
		packageName = myView.findViewById(R.id.layout_addApp_packageName_spinner_text);
		
		packagenamespinner = myView.findViewById(R.id.addApp_packageNameSpinner);
		packagenamespinner.setEnabled(false);
		
		installedCheckBox = myView.findViewById(R.id.addApp_appInstalledCheckbox);
		installedCheckBox.setOnCheckedChangeListener(this::onAppInstalledCheckbuttonCheckedChanged);
		
		packageDetailContainer = myView.findViewById(R.id.addApp_packageDetailsContainer);
		
		return myView;
	}
	
	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		sfcm.sfc.addAppFragment = this;
	}
	
	@Override
	public void onStart() {
		super.onStart();
		requireContext();
		requireActivity();
		sfcm.sfc.addAppFragment = this;
	}
	
	@Override
	public void onStop() {
		super.onStop();
		if (sfcm.sfc.addAppFragment == this)
			sfcm.sfc.addAppFragment = null;
	}
	
	@RunningInBackground
	private void onTestButtonClick() {
		new Thread(() -> {
			Activity activity = requireActivity();
			activity.runOnUiThread(() -> {
				testButton.setEnabled(false);
				testButtonProgressBar.setVisibility(View.VISIBLE);
			});
			
			String urlString = urlView.getText().toString();
			urlString = urlFilterPattern.matcher(urlString).replaceAll("");
			
			Context context = requireContext();
			if (urlString.isEmpty()) {
				toast(context, getText(R.string.err_emptyUrl), Toast.LENGTH_LONG);
				activity.runOnUiThread(() -> {
					testButtonProgressBar.setVisibility(View.INVISIBLE);
					testButton.setEnabled(true);
				});
				return;
			}
			
			if (!urlString.startsWith("http://") && !urlString.startsWith("https://") && !urlString.contains("://"))
				urlString = "https://" + urlString;
			
			String finalUrlString = urlString;
			activity.runOnUiThread(() -> urlView.setText(finalUrlString));
			
			URL url;
			try {
				url = new URL(urlString);
			} catch (MalformedURLException e) {
				toastAndLog(context, TAG,
					context.getString(R.string.addApp_testFailed) + "\n" + context.getString(R.string.err_invalidURL),
					Toast.LENGTH_SHORT, Log.DEBUG);
				activity.runOnUiThread(() -> {
					testButtonProgressBar.setVisibility(View.INVISIBLE);
					testButton.setEnabled(true);
				});
				return;
			}
			try {
				if (installedCheckBox.isChecked()) {
					tempApp = new App(tempInfo, context.getPackageManager(), url);
					tempApp.checkForUpdates();
				} else {
					String name = nameView.getText().toString();
					tempApp = new App(name, url);
				}
			} catch (IOException e) {
				toastAndLog(context, TAG, context.getString(R.string.addApp_testFailed) + "\n" + e.getMessage(), Toast.LENGTH_SHORT,
					Log.DEBUG);
				activity.runOnUiThread(() -> {
					testButtonProgressBar.setVisibility(View.INVISIBLE);
					testButton.setEnabled(true);
				});
				return;
			}
			toast(context, String.format(context.getString(R.string.addApp_testSucceed), tempApp.updateVersion.toString()),
				Toast.LENGTH_SHORT);
			
			activity.runOnUiThread(() -> {
				addButton.setEnabled(true);
				testButtonProgressBar.setVisibility(View.INVISIBLE);
				testButton.setEnabled(true);
			});
		}).start();
	}
	
	private void onAddButtonClick(@SuppressWarnings("unused") View view) {
		if (tempApp == null || tempApp.installedName.isEmpty())
			toast(view.getContext(), getString(R.string.err_emptyName), Toast.LENGTH_SHORT);
		else {
			sfcm.sfc.appList.addToList(tempApp);
			tempApp = null;
			urlView.setText("");
			nameView.setText("");
			installedCheckBox.setChecked(false);
			if (packageName != null)
				packageName.setText("");
			if (sfcm.sfc.mainActivity != null)
				sfcm.sfc.mainActivity.viewPager.setCurrentItem(1);
		}
	}
	
	@SuppressWarnings("ObjectAllocationInLoop")
	private void onAppInstalledCheckbuttonCheckedChanged(@SuppressWarnings("unused") CompoundButton compoundButton,
		boolean checked) {
		Spinner spinner = packagenamespinner;
		spinner.setEnabled(checked);
		
		if (!checked) { // clear Spinner
			SpinnerAdapter spinnerAdapter = new SimpleAdapter(sfcm.sfc.getContext(), new ArrayList<Map<String, String>>(),
				R.layout.layout_add_app_package_name_spinner, new String[]{PACKAGE_NAME},
				new int[]{R.id.layout_addApp_packageName_spinner_text});
			spinner.setAdapter(spinnerAdapter);
			
			packageDetailContainer.setVisibility(View.GONE);
			return;
		}
		
		// set up Spinner
		Context context = requireContext();
		
		PackageManager pm = context.getPackageManager();
		List<PackageInfo> installedPackages = pm.getInstalledPackages(0);
		
		Collections.sort(installedPackages, (o1, o2) -> {
			String first = o1.packageName;
			String second = o2.packageName;
			
			return first.toLowerCase().compareTo(second.toLowerCase());
		});
		
		List<Map<String, Object>> hints = new ArrayList<>();
		for (PackageInfo packageInfo : installedPackages)
			if (!appFilterPattern.matcher(packageInfo.packageName).matches()) {
				Map<String, Object> map = new HashMap<>();
				map.put(PACKAGE_INFO, packageInfo);
				map.put(PACKAGE_NAME, packageInfo.packageName);
				hints.add(map);
			}
		
		if (hints.size() == 1)
			toast(sfcm.sfc.getContext(), "Please grant permission to read installed packages", Toast.LENGTH_LONG);
		
		SpinnerAdapter spinnerAdapter =
			new SimpleAdapter(context, hints, R.layout.layout_add_app_package_name_spinner, new String[]{PACKAGE_NAME},
				new int[]{R.id.layout_addApp_packageName_spinner_text});
		
		spinner.setAdapter(spinnerAdapter);
		
		TextView version = requireActivity().findViewById(R.id.addApp_installed_version);
		TextView date = requireActivity().findViewById(R.id.addApp_installed_lastUpdated);
		
		spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@SuppressWarnings({"unchecked", "ConstantConditions"})
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				Map<String, Object> item = (Map<String, Object>) parent.getSelectedItem();
				tempInfo = (PackageInfo) item.get(PACKAGE_INFO);
				
				version.setText(tempInfo.versionName);
				nameView.setText(tempInfo.applicationInfo.loadLabel(pm));
				
				String dateAsString = Util.getDateTimeStringForInstant(tempInfo.lastUpdateTime);
				date.setText(dateAsString);
			}
			
			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			
			}
		});
		
		packageDetailContainer.setVisibility(View.VISIBLE);
	}
}
