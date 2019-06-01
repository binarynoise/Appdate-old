package de.binarynoise.appdate.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import de.binarynoise.appdate.ui.AppDetailActivity;

public class NotificationCallbackReceiver extends BroadcastReceiver {
	public static final  String ACTION_OPEN  = "open";
	public static final  String EXTRA_APP_ID = "packageName";
	private static final String TAG          = "NotificationCallback";
	
	@Override
	public void onReceive(Context context, Intent intent) {
//		sfcm.sfc.initalizeIfNotYetInitalized(context.getApplicationContext());
		
		String action = intent.getAction();
		if (ACTION_OPEN.equals(action)) {
			int id = intent.getIntExtra(EXTRA_APP_ID, 0);
			AppDetailActivity.start(context, id);
		}
	}
}
