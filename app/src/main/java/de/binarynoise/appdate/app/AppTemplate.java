package de.binarynoise.appdate.app;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import de.binarynoise.appdate.util.RunInBackground;

import static de.binarynoise.appdate.SFC.sfcm;
import static de.binarynoise.appdate.util.GoogleSheetsBridge.getValues;
import static de.binarynoise.appdate.util.GoogleSheetsBridge.updateValues;
import static de.binarynoise.appdate.util.Util.log;
import static de.binarynoise.appdate.util.Util.logPretty;

@SuppressWarnings("WeakerAccess")
public class AppTemplate implements Comparable<AppTemplate> {
	private static final  String                             TAG                  = "AppTemplate";
	private static final  LinkedHashMap<String, AppTemplate> appTemplates         = new LinkedHashMap<>();
	private static final  LinkedHashMap<String, AppTemplate> appTemplatesOnServer = new LinkedHashMap<>();
	private static        long                               lastFetched          = -1;
	public final          URL                                updateUrl;
	@NonNull public final String                             packageName;
	public final          String                             name;
	
	AppTemplate(App app) {
		if (app.installedPackageName == null)
			throw new RuntimeException("installedPackageName is null");
		packageName = app.installedPackageName;
		updateUrl = app.updateUrl;
		name = app.installedName;
	}
	
	private AppTemplate(@NonNull String packageName, String name, String updateUrlString) throws MalformedURLException {
		this.packageName = packageName;
		this.name = name;
		updateUrl = new URL(updateUrlString);
	}
	
	@RunInBackground
	public static Map<String, AppTemplate> getAvailableAppTemplates() throws IOException {
		donwloadList();
		
		AppList appList = sfcm.sfc.appList;
		HashMap<String, AppTemplate> available = new HashMap<>();
		
		for (Map.Entry<String, AppTemplate> entry : appTemplates.entrySet()) {
			AppTemplate at = entry.getValue();
			boolean installed = at.isInstalled();
			App app = appList.findByPackageName(at.packageName);
			
			if (installed && app == null)
				available.put(entry.getKey(), at);
		}
		
		return Collections.unmodifiableMap(available);
	}
	
	@RunInBackground
	public static void updateAppTemplates() throws IOException {
		App[] apps = sfcm.sfc.appList.getAll();
		
		for (App app : apps)
			if (app.installedPackageName != null && app.isInstalled()) {
				AppTemplate appTemplate = new AppTemplate(app);
				appTemplates.put(appTemplate.packageName, appTemplate);
			}
		
		uploadList();
	}
	
	@RunInBackground
	private static void donwloadList() throws IOException {
		if (System.currentTimeMillis() - lastFetched > 5000) {
			List<List<Object>> values = getValues();
			appTemplatesOnServer.clear();
			
			for (List<Object> row : values) {
				AppTemplate appTemplate = fromEntryRow(row);
				if (appTemplate != null)
					appTemplatesOnServer.put(appTemplate.packageName, appTemplate);
			}
			lastFetched = System.currentTimeMillis();
		}
		appTemplates.clear();
		appTemplates.putAll(appTemplatesOnServer);
	}
	
	@RunInBackground
	private static void uploadList() throws IOException {
		if (appTemplatesOnServer.isEmpty() || appTemplates.isEmpty() || appTemplatesOnServer.keySet().equals(appTemplates.keySet()))
			return;
		
		List<List<Object>> values = new ArrayList<>();
		
		for (AppTemplate appTemplate : appTemplates.values())
			values.add(appTemplate.asEntryRow());
		
		logPretty(TAG, appTemplates, Log.DEBUG);
		logPretty(TAG, values, Log.DEBUG);
		
		updateValues(values);
		appTemplatesOnServer.clear();
		appTemplatesOnServer.putAll(appTemplates);
		lastFetched = System.currentTimeMillis();
	}
	
	@Nullable
	private static AppTemplate fromEntryRow(List<Object> row) {
		String packageName = (String) row.get(0);
		String name = (String) row.get(1);
		String url = (String) row.get(2);
		
		try {
			return new AppTemplate(packageName, name, url);
		} catch (MalformedURLException e) {
			log(TAG, "Could not create App from Template", e, Log.WARN);
			return null;
		}
	}
	
	@Override
	public int compareTo(@NonNull AppTemplate o) {
		if (o == this)
			return 0;
		else
			return packageName.compareTo(o.packageName);
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof AppTemplate))
			return false;
		return compareTo((AppTemplate) o) == 0;
	}
	
	private boolean isInstalled() {
		Context context = sfcm.sfc.getContext();
		if (packageName.isEmpty())
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
