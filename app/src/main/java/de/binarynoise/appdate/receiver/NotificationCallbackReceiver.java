package de.binarynoise.appdate.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import de.binarynoise.appdate.ui.AppDetailActivity;

import static de.binarynoise.appdate.SFC.sfcm;

public class NotificationCallbackReceiver extends BroadcastReceiver {
	public static final  String ACTION_OPEN  = "open";
	public static final  String EXTRA_APP_ID = "packageName";
	private static final String TAG          = "NotificationCallback";
	
	@Override
	public void onReceive(Context context, Intent intent) {
		sfcm.sfc.initalizeIfNotYetInitalized(context.getApplicationContext());
		
		Log.d(TAG, "recieved intent from notification: " + intent);
		
		String action = intent.getAction();
		if (ACTION_OPEN.equals(action)) {
			long id = intent.getLongExtra(EXTRA_APP_ID, 0);
			AppDetailActivity.start(context, id);
			Log.d(TAG, String.format("started AppDetailActivity for id '%s'", id));
		}
	}
}
