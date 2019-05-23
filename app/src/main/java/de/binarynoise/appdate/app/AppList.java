package de.binarynoise.appdate.app;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.common.reflect.TypeToken;
import de.binarynoise.appdate.DownloadManagerService;
import de.binarynoise.appdate.util.RunInBackground;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;
import static de.binarynoise.appdate.SFC.sfcm;
import static de.binarynoise.appdate.util.Util.gson;
import static de.binarynoise.appdate.util.Util.log;

public class AppList {
	private static final String    SHAREDPREFSAPPLISTLABEL = "appList";
	private static final String    TAG                     = "AppList";
	private static final App[]     EMPTY_APP_ARRAY         = new App[0];
	private final        List<App> appList                 = new ArrayList<>();
	private volatile     boolean   changed                 = false;
	
	private static void upload() {
		Context context = sfcm.sfc.getContext();
		ServiceConnection serviceConnection = new ServiceConnection() {
			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {
				DownloadManagerService downloadManagerService = ((DownloadManagerService.MyBinder) service).getService();
				downloadManagerService.doInBackground(() -> {
					try {
						AppTemplate.updateAppTemplates();
					} catch (IOException e) {
						log(TAG, "could not upload templates", e, Log.WARN);
					}
				});
			}
			
			@Override
			public void onServiceDisconnected(ComponentName name) {
				context.unbindService(this);
			}
		};
		context.bindService(new Intent(context, DownloadManagerService.class), serviceConnection, Context.BIND_AUTO_CREATE);
	}
	
	public int size() {
		return appList.size();
	}
	
	public App get(int i) {
		return appList.get(i);
	}
	
	public void addToList(App app) {
		synchronized (appList) {
			appList.add(app);
			changed = true;
			sortListAndUpdate(true);
		}
	}
	
	@RunInBackground
	public void addToList(AppTemplate appTemplate) throws IOException {
		synchronized (appList) {
			appList.add(new App(appTemplate));
			changed = true;
			sortListAndUpdate(false);
		}
	}
	
	@SuppressWarnings("unused")
	public void removeFromList(App app) {
		synchronized (appList) {
			appList.remove(app);
			changed = true;
			sortListAndUpdate(true);
		}
	}
	
	public void saveChanges() {
		changed = true;
		sortListAndUpdate(true);
	}
	
	@SuppressWarnings("ObjectAllocationInLoop")
	@Nullable
	public App findByPackageName(String packageName) {
		for (App app : appList)
			if (app.installedPackageName != null && app.installedPackageName.equalsIgnoreCase(packageName))
				return app;
		
		return null;
	}
	
	@Nullable
	public App findById(long id) {
		for (App app : appList)
			if (app.id == id)
				return app;
		
		return null;
	}
	
	public App[] getAll() {
		return appList.toArray(EMPTY_APP_ARRAY);
	}
	
	public void load() {
		synchronized (appList) {
			if (!appList.isEmpty())
				return;
		}
		
		String json = getDefaultSharedPreferences(sfcm.sfc.getContext()).getString(SHAREDPREFSAPPLISTLABEL, "");
		@SuppressWarnings("UnstableApiUsage") List<App> list = gson.fromJson(json, new TypeToken<ArrayList<App>>() {}.getType());
		synchronized (appList) {
			appList.clear();
			if (list != null && !list.isEmpty())
				appList.addAll(list);
		}
	}
	
	public void sortListAndUpdate() {
		sortListAndUpdate(changed);
	}
	
	private void sortListAndUpdate(boolean upload) {
		if (!changed)
			return;
		synchronized (appList) {
			Collections.sort(appList, (o1, o2) -> {
				String first = o1.installedName;
				String second = o2.installedName;
				return first.toLowerCase().compareTo(second.toLowerCase());
			});
			
			save(upload);
			if (sfcm.sfc.appOverviewFragment != null)
				sfcm.sfc.appOverviewFragment.updateListView();
			else
				log(TAG, "ListView was null, could not update", Log.WARN);
		}
	}
	
	private void save(boolean upload) {
		if (changed) {
			Context context = sfcm.sfc.getContext();
			String json;
			synchronized (appList) {
				//noinspection UnstableApiUsage
				json = gson.toJson(appList, new TypeToken<ArrayList<App>>() {}.getType());
			}
			getDefaultSharedPreferences(context).edit().putString(SHAREDPREFSAPPLISTLABEL, json).apply();
			
			changed = false;
		}
		if (upload)
			upload();
	}
}
