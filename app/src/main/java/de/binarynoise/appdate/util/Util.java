package de.binarynoise.appdate.util;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;

import java.io.File;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Date;
import java.util.Locale;

import com.google.gson.*;
import de.binarynoise.appdate.R;

import static android.content.Context.NOTIFICATION_SERVICE;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.O;
import static de.binarynoise.appdate.SFC.sfcm;

@SuppressWarnings({"StaticMethodOnlyUsedInOneClass", "ClassNamePrefixedWithPackageName", "WeakerAccess", "unused"})
public final class Util {
	public static final  Gson          gson;
	public static final  Gson          prettyGson;
	private static final HandlerThread handlerThread = new HandlerThread("Util");
	
	static {
		gson = new GsonBuilder()
			.registerTypeAdapter(Uri.class, new Util.UriDeserializer())
			.registerTypeAdapter(Uri.class, new Util.UriSerializer())
			.create();
		prettyGson = new GsonBuilder()
			.registerTypeAdapter(Uri.class, new Util.UriDeserializer())
			.registerTypeAdapter(Uri.class, new Util.UriSerializer())
			.setPrettyPrinting()
			.disableHtmlEscaping()
			.create();
		handlerThread.start();
	}
	
	@SuppressWarnings("NumericCastThatLosesPrecision")
	public static int toProgress(long curr, long max, int steps) {
		return (int) (((double) steps) * curr / max);
	}
	
	@SuppressWarnings("unused")
	public static int toPercent(long curr, long max) {
		return toProgress(curr, max, 100);
	}
	
	public static boolean hasPogressChanged(long curr, long last, long max, @SuppressWarnings("SameParameterValue") int steps) {
		return toProgress(curr, max, steps) != toProgress(last, max, steps);
	}
	
	public static void toastAndLog(Context context, String tag, CharSequence text, Throwable t) {
		toastAndLog(context, tag, text, t, Log.DEBUG, Toast.LENGTH_SHORT);
	}
	
	public static void toastAndLog(Context context, String tag, CharSequence text, Throwable t, int duration, int level) {
		toast(context, text + ":\n" + t.getLocalizedMessage(), duration);
		log(tag, text, t, level);
	}
	
	public static void toastAndLog(Context context, String tag, CharSequence text) {
		toastAndLog(context, tag, text, Log.DEBUG, Toast.LENGTH_SHORT);
	}
	
	public static void toastAndLog(Context context, String tag, CharSequence text, int duration, int level) {
		toast(context, text, duration);
		log(tag, text, level);
	}
	
	public static void toast(Context context, CharSequence text) {
		toast(context, text, Toast.LENGTH_SHORT);
	}
	
	public static void toast(Context context, CharSequence text, int duration) {
		new Handler(handlerThread.getLooper()).post(() -> Toast.makeText(context, text, duration).show());
	}
	
	public static void log(String tag, CharSequence text, Throwable t) {
		log(tag, text, t, Log.DEBUG);
	}
	
	public static void log(String tag, CharSequence text, Throwable t, int level) {
		log(tag, text + "\n" + Log.getStackTraceString(t), level);
	}
	
	public static void log(String tag, CharSequence text) {
		log(tag, text, Log.DEBUG);
	}
	
	public static void log(String tag, CharSequence text, int level) {
		Log.println(level, "Appdate:" + tag, String.valueOf(text));
	}
	
	public static void logPretty(String tag, Object o) {
		logPretty(tag, o, Log.DEBUG);
	}
	
	public static void logPretty(String tag, Object o, int level) {
		log(tag, prettyGson.toJson(o), level);
	}
	
	@SuppressWarnings("deprecation")
	public static void notification(int id, Drawable icon, CharSequence title, CharSequence text, Intent action) {
		Context context = sfcm.sfc.getContext();
		NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
		if (notificationManager != null) {
			PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, action, 0);
			Notification.Builder builder = new Notification.Builder(context);
			builder.setContentText(text);
			builder.setAutoCancel(true);
			builder.setStyle(new Notification.BigTextStyle().bigText(text));
			builder.setLargeIcon(drawableToBitmap(icon));
			builder.setSmallIcon(R.drawable.ic_update_white_24dp);
			builder.setContentTitle(title);
			builder.setContentIntent(pendingIntent);
			
			Notification notification = builder.build();
			
			notificationManager.notify(id, notification);
		}
	}
	
	public static void clearNotification(int id) {
		Context context = sfcm.sfc.getContext();
		NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
		if (notificationManager != null)
			notificationManager.cancel(id);
	}
	
	public static Bitmap drawableToBitmap(Drawable drawable) {
		if (drawable instanceof BitmapDrawable) {
			BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
			if (bitmapDrawable.getBitmap() != null)
				return bitmapDrawable.getBitmap();
		}
		
		Bitmap bitmap;
		if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0)
			bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
		else
			bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
		
		Canvas canvas = new Canvas(bitmap);
		drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
		drawable.draw(canvas);
		return bitmap;
	}
	
	public static void dumpBundle(String tag, Bundle extras) {
		for (String s : extras.keySet())
			Log.d(tag, String.format("'%s': '%s'", s, String.valueOf(extras.get(s)).replaceAll("[\\r\\n]", "")));
	}
	
	@SuppressWarnings("SameParameterValue")
	@NonNull
	public static File getFolderForType(String type) {
		return new File(sfcm.sfc.getContext().getExternalCacheDir(), type).getAbsoluteFile();
	}
	
	@SuppressWarnings("UseOfObsoleteDateTimeApi")
	public static String getDateTimeStringForInstant(long instant) {
		return SDK_INT >= O ?
			DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).format(Instant.ofEpochMilli(instant)) :
			DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault()).format(new Date(instant));
	}
	
	public static String toAbsolutePath(URL base, String rel) throws MalformedURLException {
		return !rel.isEmpty() && rel.charAt(0) == '/' ? new URL(base, rel).toString() : rel;
	}
	
	static class UriDeserializer implements JsonDeserializer<Uri> {
		@Override
		public Uri deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
			return Uri.parse(json.toString());
		}
	}
	
	static class UriSerializer implements JsonSerializer<Uri> {
		public JsonElement serialize(Uri src, Type typeOfSrc, JsonSerializationContext context) {
			return new JsonPrimitive(src.toString());
		}
	}
}
