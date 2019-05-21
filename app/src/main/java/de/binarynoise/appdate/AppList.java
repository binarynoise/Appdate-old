package de.binarynoise.appdate;

import android.content.Context;
import android.util.Log;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gson.reflect.TypeToken;
import de.binarynoise.appdate.app.App;
import de.binarynoise.appdate.util.Util;
import net.erdfelt.android.apk.AndroidApk;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;
import static de.binarynoise.appdate.SFC.sfcm;
import static de.binarynoise.appdate.util.Util.gson;

public class AppList {
	private static final String    SHAREDPREFSAPPLISTLABEL = "appList";
	private static final String    TAG                     = "AppList";
	private static final App[]     EMPTY_APP_ARRAY         = new App[0];
	private final        List<App> appList                 = new ArrayList<>();

	public int size() {
		return appList.size();
	}

	public int getIndex(App app) {
		return appList.indexOf(app);
	}

	public App get(int i) {
		return appList.get(i);
	}

	public void addToList(App app) {
		synchronized (appList) {
			appList.add(app);
			sortListAndUpdate();
		}
	}

	@SuppressWarnings("unused")
	public void removeFromList(App app) {
		synchronized (appList) {
			appList.remove(app);
			sortListAndUpdate();
		}
	}

	public void saveChanges() {
		sortListAndUpdate();
	}

	public void load() {
		synchronized (appList) {
			if (!appList.isEmpty())
				return;
		}

		String json = getDefaultSharedPreferences(sfcm.sfc.getContext()).getString(SHAREDPREFSAPPLISTLABEL, "");
		List<App> list = gson.fromJson(json, new ArrayListTypeToken<App>().getType());
		synchronized (appList) {
			appList.clear();
			if (list != null && !list.isEmpty())
				appList.addAll(list);
		}
	}

	@SuppressWarnings("ObjectAllocationInLoop")
	@Nullable
	public App find(String name) {
		for (App app : appList)
			if (app.installedPackageName != null && app.installedPackageName.equalsIgnoreCase(name) ||
				app.installedName != null && app.installedName.equalsIgnoreCase(name))
				return app;
			else
				try {
					if (app.isDownloadValid()) {
						String updatePackageName = new AndroidApk(new File(app.cachePath)).getPackageName();
						if (updatePackageName != null && updatePackageName.equalsIgnoreCase(name))
							return app;
					}
				} catch (IOException e) {
					Util.log(TAG, "Could not instanciate AndroidApk from file\n" + app.cachePath, e, Log.WARN);
				}
		return null;
	}

	public App[] getAll() {
		return appList.toArray(EMPTY_APP_ARRAY);
	}

	private void sortListAndUpdate() {
		synchronized (appList) {
			Collections.sort(appList, (o1, o2) -> {
				String first = o1.installedName;
				String second = o2.installedName;
				return first.toLowerCase().compareTo(second.toLowerCase());
			});

			save();
			if (sfcm.sfc.appOverviewFragment != null)
				sfcm.sfc.appOverviewFragment.updateListView();
		}
	}

	private void save() {
		Context context = sfcm.sfc.getContext();
		String json;
		synchronized (appList) {
			json = gson.toJson(appList, new ArrayListTypeToken<App>().getType());
		}
		getDefaultSharedPreferences(context).edit().putString(SHAREDPREFSAPPLISTLABEL, json).apply();
	}

	private static class ArrayListTypeToken<T> extends TypeToken<ArrayList<T>> {}
}
