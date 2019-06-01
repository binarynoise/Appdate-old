package de.binarynoise.appdate.app;

import android.content.Context;
import android.util.Log;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.common.reflect.TypeToken;
import de.binarynoise.appdate.util.RunInBackground;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;
import static de.binarynoise.appdate.SFC.sfcm;
import static de.binarynoise.appdate.util.Util.gson;
import static de.binarynoise.appdate.util.Util.log;

public class AppList {
	private static final    String    SHAREDPREFSAPPLISTLABEL = "appList";
	private static final    String    TAG                     = "AppList";
	private static final    App[]     EMPTY_APP_ARRAY         = new App[0];
	private static final    List<App> appList                 = Collections.synchronizedList(new ArrayList<>());
	@SuppressWarnings("RedundantFieldInitialization") //
	private static volatile boolean   changed                 = false;
	
	@SuppressWarnings("UnstableApiUsage")
	public static void load() {
		if (!appList.isEmpty())
			return;
		
		String json = getDefaultSharedPreferences(sfcm.sfc.getContext()).getString(SHAREDPREFSAPPLISTLABEL, "");
		List<App> list = gson.fromJson(json, new TypeToken<ArrayList<App>>() {}.getType());
		
		appList.clear();
		if (list != null && !list.isEmpty())
			appList.addAll(list);
	}
	
	public static void saveChanges() {
		changed = true;
		sortListAndUpdate(true);
	}
	
	public static void sortListAndUpdate() {
		sortListAndUpdate(changed);
	}
	
	public static int size() {
		return appList.size();
	}
	
	public static App get(int i) {
		return appList.get(i);
	}
	
	public static void addToList(App app) {
		appList.add(app);
		changed = true;
		sortListAndUpdate(true);
	}
	
	@RunInBackground
	public static void addToList(AppTemplate appTemplate) throws IOException {
		appList.add(new App(appTemplate));
		changed = true;
		sortListAndUpdate(false);
	}
	
	public static void removeFromList(App app) {
		appList.remove(app);
		changed = true;
		sortListAndUpdate(true);
	}
	
	@SuppressWarnings("ObjectAllocationInLoop")
	@Nullable
	public static App findByPackageName(String packageName) {
		for (App app : appList)
			if (app.installedPackageName != null && app.installedPackageName.equalsIgnoreCase(packageName))
				return app;
		
		return null;
	}
	
	@Nullable
	public static App findById(int id) {
		for (App app : appList)
			if (app.id == id)
				return app;
		
		return null;
	}
	
	public static App[] getAll() {
		return appList.toArray(EMPTY_APP_ARRAY);
	}
	
	private static void sortListAndUpdate(boolean upload) {
		if (!changed)
			return;
		
		Collections.sort(appList, (o1, o2) -> {
			String first = o1.installedName;
			String second = o2.installedName;
			return first.toLowerCase().compareTo(second.toLowerCase());
		});
		
		save(upload);
		if (sfcm.sfc.appOverviewFragment != null)
			sfcm.sfc.appOverviewFragment.updateListView();
	}
	
	@SuppressWarnings("UnstableApiUsage")
	private static void save(boolean upload) {
		if (changed) {
			Context context = sfcm.sfc.getContext();
			String json = gson.toJson(appList, new TypeToken<ArrayList<App>>() {}.getType());
			getDefaultSharedPreferences(context).edit().putString(SHAREDPREFSAPPLISTLABEL, json).apply();
			changed = false;
		}
		if (upload)
			upload();
	}
	
	private static void upload() {
		new Thread(() -> {
			try {
				AppTemplate.updateAppTemplates();
			} catch (IOException e) {
				log(TAG, "could not upload templates", e, Log.WARN);
			}
		}).start();
	}
}
