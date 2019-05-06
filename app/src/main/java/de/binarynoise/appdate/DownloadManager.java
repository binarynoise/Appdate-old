package de.binarynoise.appdate;

import android.net.ConnectivityManager;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;

import de.binarynoise.appdate.callbacks.ProgressCallback;
import de.binarynoise.appdate.util.RunInBackground;
import de.binarynoise.appdate.util.Tupel;

import static de.binarynoise.appdate.SFC.sfcm;
import static de.binarynoise.appdate.util.Util.hasPogressChanged;

public class DownloadManager {
	@SuppressWarnings("unused")
	private static final String TAG = "DownloadManager";
	
	@RunInBackground
	static Tupel<String, Long> downloadToFile(String downloadURLString, File destFolder, ProgressCallback progressCallback)
		throws IOException {
		
		String absolutePath = createAbsolutePath(downloadURLString, destFolder);
		
		File dest = new File(absolutePath);
		
		if (!dest.getParentFile().exists() && !dest.getParentFile().mkdirs())
			throw new IOException(
				String.format("Could not create file '%s' because the parent folder(s) could not be created", absolutePath));
		if (!dest.exists() && !dest.createNewFile())
			throw new IOException(String.format("File '%s' could not be created", absolutePath));
		
		ConnectivityManager connectivityManager = sfcm.sfc.getContext().getSystemService(ConnectivityManager.class);
		if (connectivityManager == null || !connectivityManager.getActiveNetworkInfo().isConnected()) {
			IOException e = new IOException("no network connection found. Please try again later");
			if (!dest.exists() || dest.delete())
				throw e;
			String message =
				String.format("Could not delete file '%s' after IO-ErrorCallback. Please delete manually.", absolutePath);
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
}
