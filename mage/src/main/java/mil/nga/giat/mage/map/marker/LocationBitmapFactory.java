package mil.nga.giat.mage.map.marker;

import android.animation.ArgbEvaluator;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
import android.os.Build;
import android.util.Log;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;

import java.io.IOException;
import java.io.InputStream;

import mil.nga.giat.mage.sdk.datastore.location.Location;
import mil.nga.giat.mage.sdk.datastore.user.User;

public class LocationBitmapFactory {

	private static Long upperBoundTimeInSeconds = 1800L;
	
	private static final String LOG_NAME = LocationBitmapFactory.class.getName();
	
	// TODO : SCOTT UFM
	private static final Integer[] colorGradient = { 0xff0000ff, 0xffffff00 ,0xffffa500 };
	
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	public static Bitmap bitmap(Context context, Location location, User user) {
		Bitmap bitmap = createDot(context, location, user);
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
		Integer width = Math.max(dot.getWidth(), icon.getWidth());
		Integer height = (dot.getHeight() / 2) + icon.getHeight();		
		
		Bitmap combined = Bitmap.createBitmap(width, height, Config.ARGB_8888);
		Canvas c = new Canvas(combined);
		
		c.drawBitmap(dot, (width-dot.getWidth())/2, height - dot.getHeight(), null);		
		c.drawBitmap(icon, (width-icon.getWidth())/2, 0, null);
		
		return combined;
	}
	
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	public static Bitmap bitmapUser(Context context, String iconPath) {
		Bitmap bitmap = BitmapFactory.decodeFile(iconPath);
		if (bitmap == null) {
			return null;
		}

		Integer maxDimension = Math.max(bitmap.getWidth(), bitmap.getHeight());
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
	
	public static BitmapDescriptor dotBitmapDescriptor(Context context, Location location, User user) {
		Bitmap bitmap = createDot(context, location, user);
		return BitmapDescriptorFactory.fromBitmap(bitmap);
	}
	
	public static Bitmap createDot(Context context, Location location, User user) {
		Bitmap dotBitmap = null;
		InputStream is = null;
		
		try {
			Long interval = (System.currentTimeMillis() - location.getTimestamp().getTime()) / 1000l;
			// max out at 30 minutes
			interval = Math.min(interval, upperBoundTimeInSeconds);

			// upper bound in minutes
			Double u = upperBoundTimeInSeconds.doubleValue() / 60.0;

			// non-linear lookup
			// 0 mins = blue
			// 10 mins = yellow
			// 30 mins = orange
			Double gradientIndexDecimalNormalized = -0.25 + Math.sqrt((u * u) + (u * 24 * (interval.doubleValue() / 60.0))) / (4 * u);
			// between 0 and 1
			gradientIndexDecimalNormalized = Math.min(Math.max(0.0, gradientIndexDecimalNormalized), 1.0);

			// find the index into the gradient
			int gradientIndex = (int) (gradientIndexDecimalNormalized * (double) (colorGradient.length - 1));

			// linearly interpolate between the gradient index
			Integer COLOR1 = colorGradient[gradientIndex];
			Integer COLOR2 = colorGradient[Math.min(gradientIndex + 1, colorGradient.length - 1)];
			
			// TODO : this use to use a gradient, but no longer do. Could remove some of this code
			int color = (Integer) new ArgbEvaluator().evaluate(gradientIndexDecimalNormalized.floatValue(), COLOR1, COLOR1);

			// use a invert filter to swap black and white colors in the bitmap. This will preserve the original black
			float[] colorMatrix_Negative = { -1.0f, 0, 0, 0, 255, 0, -1.0f, 0, 0, 255, 0, 0, -1.0f, 0, 255, 0, 0, 0, 1.0f, 0 };

			// make a mutable copy of the bitmap
			is = context.getAssets().open("dots/black_dot.png");
			Bitmap bitmap = BitmapFactory.decodeStream(is);

			// scale the image to a good size
			Integer maxDimension = Math.max(bitmap.getWidth(), bitmap.getHeight());
			float density = context.getResources().getDisplayMetrics().xdpi; //context.getResources().getDisplayMetrics().densityDpi;
			double scale = (density/10.0) / maxDimension;
			int outWidth = Double.valueOf(scale*Integer.valueOf(bitmap.getWidth()).doubleValue()).intValue();
			int outHeight = Double.valueOf(scale*Integer.valueOf(bitmap.getHeight()).doubleValue()).intValue();
			bitmap = Bitmap.createScaledBitmap(bitmap, outWidth, outHeight, true);
			
			dotBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);

			Canvas myCanvas = new Canvas(dotBitmap);
			// invert the gradient first
			Paint gradientPaint = new Paint();
			gradientPaint.setColorFilter(new LightingColorFilter(Color.WHITE, 0xFFFFFFFF - color));
			myCanvas.drawBitmap(dotBitmap, 0, 0, gradientPaint);
			// invert the entire image second
			Paint negativePaint = new Paint();
			negativePaint.setColorFilter(new ColorMatrixColorFilter(new ColorMatrix(colorMatrix_Negative)));
			myCanvas.drawBitmap(dotBitmap, 0, 0, negativePaint);
		} catch (IOException e1) {
			try {
				dotBitmap = BitmapFactory.decodeStream(context.getAssets().open("dots/maps_dav_bw_dot.png"));
				
				// scale the image to a good size
				Integer maxDimension = Math.max(dotBitmap.getWidth(), dotBitmap.getHeight());
				float density = context.getResources().getDisplayMetrics().xdpi; //context.getResources().getDisplayMetrics().densityDpi;
				double scale = (density/3.5) / maxDimension;
				int outWidth = Double.valueOf(scale*Integer.valueOf(dotBitmap.getWidth()).doubleValue()).intValue();
				int outHeight = Double.valueOf(scale*Integer.valueOf(dotBitmap.getHeight()).doubleValue()).intValue();
				dotBitmap = Bitmap.createScaledBitmap(dotBitmap, outWidth, outHeight, true);
			} catch (IOException e2) {
				Log.e(LOG_NAME, "Problem setting generating bitmap", e2);
			}
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException ignore) {
				}
			}
		}


		return dotBitmap;
	}

}