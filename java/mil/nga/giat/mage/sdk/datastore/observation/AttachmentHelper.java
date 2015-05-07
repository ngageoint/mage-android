package mil.nga.giat.mage.sdk.datastore.observation;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.common.io.Files;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.SecureRandom;

import mil.nga.giat.mage.sdk.R;
import mil.nga.giat.mage.sdk.datastore.DaoStore;
import mil.nga.giat.mage.sdk.utils.MediaUtility;

public class AttachmentHelper {

	private static final String LOG_NAME = AttachmentHelper.class.getName();

	private static final SecureRandom random = new SecureRandom();

	/**
	 * This method will attempt to correctly rotate and format an image in preparation for display or uploading.
	 * 
	 * @param attachment
	 * @param context
	 */
	public static void stageForUpload(Attachment attachment, Context context) throws Exception {
		File stageDir = MediaUtility.getMediaStageDirectory();
		File inFile = new File(attachment.getLocalPath());
		// add random string to the front of the filename to avoid conflicts
		File stagedFile = new File(stageDir, new BigInteger(30, random).toString(32) + new File(attachment.getLocalPath()).getName());

		Log.d(LOG_NAME, "Staging file: " + stagedFile.getAbsolutePath());
		Log.d(LOG_NAME, "Local path is: " + attachment.getLocalPath());
		if (stagedFile.getAbsolutePath().equalsIgnoreCase(attachment.getLocalPath())) {
			Log.d(LOG_NAME, "Attachment is already staged.  Nothing to do.");
			return;
		}
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		Integer outImageSize = sharedPreferences.getInt(context.getString(R.string.imageUploadSizeKey), context.getResources().getInteger(R.integer.imageUploadSizeDefaultValue));

		if (MediaUtility.isImage(stagedFile.getAbsolutePath())) {

			// if not original image size
			if (outImageSize > 0) {

				BitmapFactory.Options options = new BitmapFactory.Options();
				options.inPreferredConfig = Bitmap.Config.RGB_565;
				options.inSampleSize = 2;
				Bitmap bitmap = BitmapFactory.decodeFile(inFile.getAbsolutePath(), options);

				// Scale file
				Integer inWidth = bitmap.getWidth();
				Integer inHeight = bitmap.getHeight();

				Integer outWidth = outImageSize;
				Integer outHeight = outImageSize;

				if (inWidth > inHeight) {
					outWidth = outImageSize;
					outHeight = ((Double) ((inHeight.doubleValue() / inWidth.doubleValue()) * outImageSize.doubleValue())).intValue();
				} else if (inWidth < inHeight) {
					outWidth = ((Double) ((inWidth.doubleValue() / inHeight.doubleValue()) * outImageSize.doubleValue())).intValue();
					outHeight = outImageSize;
				}
				bitmap = Bitmap.createScaledBitmap(bitmap, outWidth, outHeight, true);

				// TODO: TESTING, might still run out of memory...
				if(outImageSize <= 1024) {
					bitmap = MediaUtility.orientBitmap(bitmap, inFile.getAbsolutePath(), true);
				}

				OutputStream out = new FileOutputStream(stagedFile);
				bitmap.compress(CompressFormat.JPEG, 100, out);

				out.flush();
				out.close();
				bitmap.recycle();
				MediaUtility.copyExifData(inFile, stagedFile);
			} else {
				Files.copy(inFile, stagedFile);
			}
		} else {
			Files.copy(inFile, stagedFile);
		}
		attachment.setLocalPath(stagedFile.getAbsolutePath());
		DaoStore.getInstance(context).getAttachmentDao().update(attachment);
	}
}
