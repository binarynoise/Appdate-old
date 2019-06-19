package de.binarynoise.appdate.receiver;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.reflect.TypeToken;
import com.google.gson.JsonParseException;
import de.binarynoise.appdate.ui.AppDetailActivity;
import de.binarynoise.appdate.util.Util;

import static de.binarynoise.appdate.util.Util.log;

public class NotificationCallbackReceiver extends BroadcastReceiver {
	public static final  String ACTION_OPEN_APP_DETAIL = "ACTION_OPEN_APP_DETAIL";
	public static final  String EXTRA_APP_ID           = "APP_ID";
	public static final  String ACTION                 = "ACTION";
	private static final String TAG                    = "NotificationCallback";
	
	@Override
	public void onReceive(Context context, Intent intent) {
		try {
			NotificationActionWithExtras extras = new NotificationActionWithExtras(intent.getAction());
			if (ACTION_OPEN_APP_DETAIL.equals(extras.get(ACTION))) {
				String s = extras.get(EXTRA_APP_ID);
				if (s != null && !s.isEmpty()) {
					int id = Integer.parseInt(s);
					AppDetailActivity.start(context, id);
				}
			}
		} catch (ClassCastException | JsonParseException | NumberFormatException ex) {
			log(TAG, "invalid notification action: " + intent.getAction(), ex, Log.WARN);
		}
	}
	
	/**
	 * this class allows us to create {@link PendingIntent PendingIntents} with the same action but different extras
	 * that would be lost, if different from existring PendingIntents with same action.
	 */
	@SuppressWarnings("UnstableApiUsage")
	public static class NotificationActionWithExtras {
		private final Map<String, String> extras = new ConcurrentHashMap<>(new LinkedHashMap<>(2));
		
		public NotificationActionWithExtras() {}
		
		public NotificationActionWithExtras(@Nullable Map<String, String> extras) {
			if (extras != null)
				for (Map.Entry<String, String> entry : extras.entrySet())
					this.extras.put(entry.getKey(), entry.getValue());
		}
		
		public NotificationActionWithExtras(@Nullable String extras) throws JsonParseException {
			this(Util.gson.<Map<String, String>>fromJson(extras, new TypeToken<Map<String, String>>() {}.getType()));
		}
		
		@NonNull
		@Override
		public String toString() {
			return Util.gson.toJson(extras);
		}
		
		public void put(String key, Object value) {
			extras.put(key, value.toString());
		}
		
		public void put(String key, String value) {
			extras.put(key, value);
		}
		
		@Nullable
		public String get(String key) {
			return extras.get(key);
		}
		
		public Intent toIntent(Context context) {
			Intent intent = new Intent(context, NotificationCallbackReceiver.class);
			intent.setAction(toString());
			return intent;
		}
	}
}
