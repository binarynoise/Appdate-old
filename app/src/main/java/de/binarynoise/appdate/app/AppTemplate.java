package de.binarynoise.appdate.app;

import android.content.Context;
import android.content.pm.PackageManager;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import de.binarynoise.appdate.AppList;
import de.binarynoise.appdate.util.RunInBackground;

import static de.binarynoise.appdate.SFC.sfcm;
import static de.binarynoise.appdate.util.GoogleSheetsBridge.getValues;
import static de.binarynoise.appdate.util.GoogleSheetsBridge.updateValues;

@SuppressWarnings("WeakerAccess")
public class AppTemplate {
	private static final HashMap<String, AppTemplate> appTemplates = new HashMap<>();
	public final         URL                          updateUrl;
	public final         String                       packageName;
	public final         String                       name;
	
	public AppTemplate(App app) {
		if (app.installedPackageName == null)
			throw new RuntimeException("installedPackageName is null");
		packageName = app.installedPackageName;
		updateUrl = app.updateUrl;
		name = app.installedName;
	}
	
	private AppTemplate(String packageName, String name, String updateUrlString) throws MalformedURLException {
		this.packageName = packageName;
		this.name = name;
		updateUrl = new URL(updateUrlString);
	}
	
	public static Map<String, AppTemplate> getAvailableAppTemplates() throws IOException {
		donwloadList();
		
		AppList appList = sfcm.sfc.appList;
		HashMap<String, AppTemplate> available = new HashMap<>();
		for (Map.Entry<String, AppTemplate> entry : appTemplates.entrySet()) {
			AppTemplate at = entry.getValue();
			if (at.isInstalled() && appList.find(at.packageName) != null)
				available.put(entry.getKey(), at);
		}
		
		return Collections.unmodifiableMap(available);
	}
	
	public static void updateAppTemplates() throws IOException {
		App[] apps = sfcm.sfc.appList.getAll();
		
		for (App app : apps)
			if (app.installedPackageName != null && app.isInstalled())
				appTemplates.put(app.installedName, new AppTemplate(app));
		
		uploadList();
	}
	
	@RunInBackground
	private static void donwloadList() throws IOException {
		List<List<Object>> values = getValues();
		
		for (List<Object> row : values) {
			AppTemplate appTemplate = fromEntryRow(row);
			appTemplates.put(appTemplate.packageName, appTemplate);
		}
	}
	
	@RunInBackground
	private static void uploadList() throws IOException {
		List<List<Object>> values = new ArrayList<>();
		for (AppTemplate at : appTemplates.values())
			values.add(at.asEntryRow());
		
		updateValues(values);
	}
	
	private static AppTemplate fromEntryRow(List<Object> row) throws MalformedURLException {
		String packageName = (String) row.get(0);
		String name = (String) row.get(0);
		String url = (String) row.get(0);
		
		return new AppTemplate(packageName, name, url);
	}
	
	public boolean isInstalled() {
		Context context = sfcm.sfc.getContext();
		if (packageName == null || packageName.isEmpty())
			return false;
		PackageManager packageManager = context.getPackageManager();
		try {
			packageManager.getPackageInfo(packageName, 0);
			return true;
		} catch (PackageManager.NameNotFoundException e) {
			return false;
		}
	}
	
	private List<Object> asEntryRow() {
		return Arrays.asList(packageName, name, updateUrl.toString());
	}
}
