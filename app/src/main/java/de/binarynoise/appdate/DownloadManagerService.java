package de.binarynoise.appdate;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Process;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.IOException;

import de.binarynoise.appdate.callbacks.ErrorCallback;
import de.binarynoise.appdate.callbacks.ProgressCallback;
import de.binarynoise.appdate.callbacks.ResultCallback;
import de.binarynoise.appdate.callbacks.SuccessCallback;
import de.binarynoise.appdate.util.RunningInBackground;
import de.binarynoise.appdate.util.ServiceHandlerThread;
import de.binarynoise.appdate.util.Tupel;

public class DownloadManagerService extends Service {
	private static final String               TAG = "DownloadManagerService";
	private              ServiceHandlerThread handlerThread;
	private              Handler              handler;
	
	@RunningInBackground
	public void downloadInBackground(String downloadURLString, File destFolder, ResultCallback<Tupel<String, Long>> resultCallback,
		SuccessCallback successCallback, ErrorCallback errorCallback, ProgressCallback progressCallback) {
		doInBackground(() -> {
			try {
				Tupel<String, Long> t = DownloadManager.downloadToFile(downloadURLString, destFolder, progressCallback);
				resultCallback.onResult(t, successCallback, errorCallback);
			} catch (IOException e) {
				errorCallback.onError(e);
			}
		});
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		handlerThread = new ServiceHandlerThread("DownloadManagerHandlerThread", Process.THREAD_PRIORITY_BACKGROUND, this);
		handlerThread.start();
		handler = new Handler(handlerThread.getLooper());
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return START_STICKY;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		handlerThread.quitSafely();
	}
	
	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	public void doInBackground(Runnable runnable) {
		handler.post(runnable);
	}
	
	public static class MyBinder extends Binder {
		private final DownloadManagerService service;
		
		private MyBinder(DownloadManagerService service) {
			
			this.service = service;
		}
		
		public DownloadManagerService getService() {
			return service;
		}
	}
}
