package de.binarynoise.appdate;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.net.ConnectivityManager;
import android.util.SparseArray;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;

import de.binarynoise.appdate.app.App;
import de.binarynoise.appdate.app.AppList;
import de.binarynoise.appdate.callbacks.ProgressCallback;
import de.binarynoise.appdate.util.RunInBackground;
import de.binarynoise.appdate.util.RunningInBackground;
import de.binarynoise.appdate.util.Tupel;
import de.binarynoise.appdate.util.Util;

import static de.binarynoise.appdate.SFC.sfcm;
import static de.binarynoise.appdate.util.Util.hasPogressChanged;

public class DownloadManagerService extends JobService {
	public static final  String              EXTRA_APP_ID = "APP_ID";
	private static final String              TAG          = "DownloadManagerService";
	private final        SparseArray<Thread> threads      = new SparseArray<>();
	
	@RunInBackground
	private static Tupel<String, Long> downloadToFile(String downloadURLString, File destFolder, ProgressCallback progressCallback)
		throws IOException {
		String absolutePath = createAbsolutePath(downloadURLString, destFolder);
		File dest = new File(absolutePath);
		
		if (!dest.getParentFile().exists() && !dest.getParentFile().mkdirs())
			throw new IOException(
				String.format("Could not create file '%s' because the parent folder(s) could not be created", absolutePath));
		if (!dest.exists() && !dest.createNewFile())
			throw new IOException(String.format("File '%s' could not be created", absolutePath));
		
		ConnectivityManager connectivityManager =
			(ConnectivityManager) sfcm.sfc.getContext().getSystemService(CONNECTIVITY_SERVICE);
		
		if (connectivityManager == null || !connectivityManager.getActiveNetworkInfo().isConnected()) {
			IOException e = new IOException("no network connection found. Please try again later"); //TODO
			if (!dest.exists() || dest.delete())
				throw e;
			String message =
				String.format("Could not delete file '%s' after IO-ErrorCallback. Please delete manually.", absolutePath); // TODO
			throw new IOException(message, e);
		}
		
		URL src = new URL(downloadURLString);
		URLConnection urlConnection = src.openConnection();
		
		byte[] buffer = new byte[0x100000]; // 1MB
		try (BufferedInputStream bufferedInputStream = new BufferedInputStream(urlConnection.getInputStream());
			BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(absolutePath, false))) {
			
			long size = urlConnection.getContentLength();
			long progress = 0;
			
			int count;
			while ((count = bufferedInputStream.read(buffer)) != -1) {
				bufferedOutputStream.write(buffer, 0, count);
				progress += count;
				if (hasPogressChanged(progress, progress - count, size, 1000))
					progressCallback.onProgress(progress, size);
			}
			return new Tupel<>(absolutePath, size);
		} catch (IOException e) {
			if (dest.exists() && !dest.delete())
				e.addSuppressed(new IOException(
					String.format("Could not delete file '%s' after IO-ErrorCallback. Please delete manually.", absolutePath)));
			throw e;
		}
	}
	
	private static String createAbsolutePath(String destFile, File destFolder) {
		String[] split = destFile.split("/");
		String fileName = split[split.length - 1];
		return new File(destFolder, fileName).getAbsolutePath();
	}
	
	@RunningInBackground
	@Override
	public boolean onStartJob(JobParameters params) {
		sfcm.sfc.initalizeIfNotYetInitalized(getApplicationContext());
		
		int id = params.getExtras().getInt(EXTRA_APP_ID);
		App app = AppList.findById(id);
		
		if (app == null)
			return false;
		else {
			Thread thread = new Thread(() -> {
				try {
					Tupel<String, Long> t =
						downloadToFile(app.downloadURLString, Util.getFolderForType("apk"), app::onDownloadProgress);
					app.onResult(t, app::onDownloadSuccess, app::onDownloadError);
					jobFinished(params, false);
				} catch (IOException e) {
					app.onDownloadError(e);
					jobFinished(params, true);
				}
				threads.remove(id);
			});
			threads.put(id, thread);
			thread.start();
			
			return true;
		}
	}
	
	@Override
	public boolean onStopJob(JobParameters params) {
		int id = params.getExtras().getInt(EXTRA_APP_ID);
		App app = AppList.findById(id);
		if (app == null)
			return false;
		threads.get(id).interrupt();
		threads.remove(id);
		return true;
	}
}
