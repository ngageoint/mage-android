package mil.nga.giat.mage.map.marker;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;

import java.util.HashMap;
import java.util.Map;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.sdk.datastore.location.Location;
import mil.nga.giat.mage.sdk.datastore.user.User;

public class LocationBitmapFactory {

	private static final int MIN_BOUND_SECONDS = 600;
	private static final int MAX_BOUND_SECONDS = 1200;

	private static final int DOT_DIMENSION = 18;
	private static final int DOT_RADIUS = 8;

	private static Map<Integer, Bitmap> DOTS = new HashMap<>();

	private static final String LOG_NAME = LocationBitmapFactory.class.getName();

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	public static Bitmap bitmap(Context context, Location location, User user) {
		Bitmap bitmap = createDot(context, location);
		Log.d(LOG_NAME, "Drawing the bitmap for user " + user.getDisplayName());
		final String iconPath = user.getUserLocal().getLocalIconPath();

		if (iconPath != null) {
			Bitmap userIcon = bitmapUser(context, iconPath);
			if (userIcon != null) {
				bitmap = combineIconAndDot(bitmap.copy(Bitmap.Config.ARGB_8888, true), userIcon);
			}
		}
		
		return bitmap;
	}

	private static Bitmap combineIconAndDot(Bitmap dot, Bitmap icon) {
		int width = Math.max(dot.getWidth(), icon.getWidth());
		int height = (dot.getHeight() / 2) + icon.getHeight();
		
		Bitmap combined = Bitmap.createBitmap(width, height, Config.ARGB_8888);
		Canvas c = new Canvas(combined);
		
		c.drawBitmap(dot, (width - dot.getWidth()) / 2f, height - dot.getHeight(), null);
		c.drawBitmap(icon, (width - icon.getWidth()) / 2f, 0, null);
		
		return combined;
	}
	
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	private static Bitmap bitmapUser(Context context, String iconPath) {
		Bitmap bitmap = BitmapFactory.decodeFile(iconPath);
		if (bitmap == null) {
			return null;
		}

		int maxDimension = Math.max(bitmap.getWidth(), bitmap.getHeight());
		float density = context.getResources().getDisplayMetrics().xdpi;
		double scale = (density/3.5) / maxDimension;
		int outWidth = Double.valueOf(scale*Integer.valueOf(bitmap.getWidth()).doubleValue()).intValue();
		int outHeight = Double.valueOf(scale*Integer.valueOf(bitmap.getHeight()).doubleValue()).intValue();
		bitmap = Bitmap.createScaledBitmap(bitmap, outWidth, outHeight, true);

		return bitmap;
	}

	public static BitmapDescriptor bitmapDescriptor(Context context, Location location, User user) {
		Bitmap bitmap = bitmap(context, location, user);
		return BitmapDescriptorFactory.fromBitmap(bitmap);
	}
	
	public static BitmapDescriptor dotBitmapDescriptor(Context context, Location location) {
		Bitmap bitmap = createDot(context, location);
		return BitmapDescriptorFactory.fromBitmap(bitmap);
	}

	public static int locationColor(Context context, Location location) {
		long interval = (System.currentTimeMillis() - location.getTimestamp().getTime()) / 1000L;
		if (interval <= MIN_BOUND_SECONDS) {
			return ContextCompat.getColor(context, R.color.location_circle_fill_min);
		} else if (interval <= MAX_BOUND_SECONDS) {
			return ContextCompat.getColor(context, R.color.location_circle_fill_intermediate);
		} else {
			return ContextCompat.getColor(context, R.color.location_circle_fill_max);
		}
	}
	
	private static Bitmap createDot(Context context, Location location) {
		int color = locationColor(context, location);
		Bitmap bitmap = DOTS.get(color);
		if (bitmap != null) {
			return bitmap;
		}

		float density = context.getResources().getDisplayMetrics().density;
		int dimension = (int) (DOT_DIMENSION * density);
		int radius = (int) (DOT_RADIUS * density);

		bitmap = Bitmap.createBitmap(dimension, dimension, Bitmap.Config.ARGB_8888);

		Canvas canvas = new Canvas(bitmap);
		Paint paint = new Paint();
		paint.setAntiAlias(true);

		paint.setStyle(Paint.Style.FILL);
		paint.setColor(color);
		canvas.drawCircle(dimension / 2f, dimension / 2f, radius, paint);

		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeWidth(4);
		paint.setColor(Color.WHITE);
		canvas.drawCircle(dimension / 2f, dimension / 2f, radius, paint);

		canvas.drawBitmap(bitmap, 0, 0, paint);

		DOTS.put(color, bitmap);

		return bitmap;
	}

}