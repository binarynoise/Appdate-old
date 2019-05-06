package de.binarynoise.appdate.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import de.binarynoise.appdate.SFC;
import de.binarynoise.appdate.util.Util;

public class BootCompletedReceiver extends BroadcastReceiver {
	private static final String TAG = "BootCompletedReciver";
	
	@Override
	public void onReceive(Context context, Intent intent) {
		if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()))
			return;
		Util.log(TAG, "recieved boot completed", Log.DEBUG);
		SFC.sfcm.sfc.initalizeIfNotYetInitalized(context.getApplicationContext());
	}
}
