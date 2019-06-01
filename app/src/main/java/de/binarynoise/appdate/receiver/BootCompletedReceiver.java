package de.binarynoise.appdate.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import de.binarynoise.appdate.SFC;

public class BootCompletedReceiver extends BroadcastReceiver {
	private static final String TAG = "BootCompletedReciver";
	
	@Override
	public void onReceive(Context context, Intent intent) {
		if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()))
			return;
		SFC.sfcm.sfc.initalizeIfNotYetInitalized(context.getApplicationContext());
	}
}
