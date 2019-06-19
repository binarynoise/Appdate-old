package de.binarynoise.appdate.app;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import com.google.common.reflect.TypeToken;
import de.binarynoise.appdate.ui.AppOverviewFragment;
import de.binarynoise.appdate.util.RunInBackground;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;
import static de.binarynoise.appdate.SFC.sfcm;
import static de.binarynoise.appdate.receiver.NotificationCallbackReceiver.*;
import static de.binarynoise.appdate.util.Util.*;

public class AppList {
	private static final    String        SHAREDPREFSAPPLISTLABEL = "appList";
	private static final    String        TAG                     = "AppList";
	private static final    App[]         EMPTY_APP_ARRAY         = new App[0];
	private static final    List<App>     appList                 = Collections.synchronizedList(new ArrayList<>());
	@SuppressWarnings("RedundantFieldInitialization") //
	private static volatile boolean       changed                 = false;
	private static final    ReentrantLock lock                    = new ReentrantLock(true);
	
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
	
	public static void addToList(@NonNull App app) {
		// to prevent duplicate apps
		App old = findByPackageName(app.installedPackageName);
		if (old != null)
			old.delete();
		
		lock.lock();
		try {
			appList.add(app);
		} finally {
			lock.unlock();
		}
		changed = true;
		sortListAndUpdate(true);
	}
	
	@RunInBackground
	public static void addToList(AppTemplate appTemplate) throws IOException {
		App app = new App(appTemplate);
		lock.lock();
		try {
			appList.add(app);
		} finally {
			lock.unlock();
		}
		changed = true;
		sortListAndUpdate(false);
	}
	
	@SuppressWarnings("ObjectAllocationInLoop")
	@Nullable
	public static App findByPackageName(@Nullable String packageName) {
		if (packageName == null)
			return null;
		for (App app : appList)
			if (packageName.equalsIgnoreCase(app.installedPackageName))
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
	
	public static void checkForUpdates() {
		AppOverviewFragment.setRefreshing(true);
		lock.lock();
		try {
			for (App app : appList) {
				try {
					app.checkForUpdates();
				} catch (IOException ignored) {}
				if (app.hasUpdates) {
					NotificationActionWithExtras extras = new NotificationActionWithExtras();
					extras.put(EXTRA_APP_ID, app.id);
					extras.put(ACTION, ACTION_OPEN_APP_DETAIL);
					Intent action = extras.toIntent(sfcm.sfc.getContext());
					
					String title = String.format("Update for %s", app.installedName);
					
					String text;
					if (app.isInstalled())
						text = String.format(
							"Appdate has found an update for app %s.\nThe currently installed Version is %s\nThe available version is %s",
							app.installedName, app.installedVersion, app.updateVersion);
					else
						text = String.format("Appdate can install app %s.\nThe available version is %s", app.installedName,
							app.updateVersion);
					
					Drawable drawable = app.getIcon();
					
					int id = app.id;
					notification(id, drawable, title, text, action);
				}
			}
			saveChanges();
			AppOverviewFragment.setRefreshing(false);
		} finally {
			lock.unlock();
		}
	}
	
	static void removeFromList(App app) {
		lock.lock();
		try {
			appList.remove(app);
		} finally {
			lock.unlock();
		}
		changed = true;
		sortListAndUpdate(true);
	}
	
	static App[] getAll() {
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
		if (lock.tryLock()) {
			new Thread(() -> {
				try {
					AppTemplate.updateAppTemplates();
				} catch (IOException e) {
					log(TAG, "could not upload templates", e, Log.WARN);
				}
			}).start();
			lock.unlock();
		}
	}
}
