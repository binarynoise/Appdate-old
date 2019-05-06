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
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;

import java.io.File;
import java.lang.reflect.Type;
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

@SuppressWarnings({"StaticMethodOnlyUsedInOneClass", "ClassNamePrefixedWithPackageName"})
public final class Util {
	public static final  Gson gson;
	public static final  Gson prettyGson;
	private static final int  NOTIFICATION_ID = 12345689;
	
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
	
	public static void toastAndLog(Context context, String tag, CharSequence text, Throwable t, int duration, int level) {
		toast(context, text + ":\n" + t.getMessage(), duration);
		log(tag, text, t, level);
	}
	
	public static void toastAndLog(Context context, String tag, CharSequence text, int duration, int level) {
		toast(context, text, duration);
		log(tag, text, level);
	}
	
	public static void toast(Context context, CharSequence text, int duration) {
		new Thread(() -> {
			Looper.prepare();
			
			Toast toast = Toast.makeText(context, text, duration);
			toast.show();
			
			Looper.loop();
			Looper.getMainLooper().quitSafely();
		}).start();
	}
	
	public static void log(String tag, CharSequence text, Throwable t, int level) {
		log(tag, text + "\n" + Log.getStackTraceString(t), level);
	}
	
	public static void log(String tag, CharSequence text, int level) {
		Log.println(level, tag, String.valueOf(text));
	}
	
	@SuppressWarnings("deprecation")
	public static void notification(Drawable icon, CharSequence title, CharSequence text, Intent action) {
		Context context = sfcm.sfc.getContext();
		NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
		if (notificationManager != null) {
			PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, action, 0);
			Notification.Builder builder = new Notification.Builder(context);
			builder.setContentText(text);
//			builder.setAutoCancel(true);
			builder.setStyle(new Notification.BigTextStyle().bigText(text));
//			builder.addAction(new Notification.Action.Builder(R.drawable.ic_update_white_24dp, title, pendingIntent).build());
			builder.setLargeIcon(drawableToBitmap(icon));
			builder.setSmallIcon(R.drawable.ic_update_white_24dp);
			builder.setContentTitle(title);
			builder.setContentIntent(pendingIntent);
			
			Notification notification = builder.build();
			
			notificationManager.notify(NOTIFICATION_ID, notification);
		}
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
	
	@SuppressWarnings("unused")
	public static void dumpBundle(String tag, Bundle extras) {
		extras
			.keySet()
			.iterator()
			.forEachRemaining(
				s -> Log.d(tag, String.format("'%s': '%s'", s, String.valueOf(extras.get(s)).replaceAll("[\\r\\n]", ""))));
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
