package mil.nga.giat.mage.sdk.datastore.observation;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.SecureRandom;

import mil.nga.giat.mage.sdk.R;
import mil.nga.giat.mage.sdk.datastore.DaoStore;
import mil.nga.giat.mage.sdk.utils.MediaUtility;

import org.apache.sanselan.Sanselan;
import org.apache.sanselan.common.IImageMetadata;
import org.apache.sanselan.formats.jpeg.JpegImageMetadata;
import org.apache.sanselan.formats.jpeg.exifRewrite.ExifRewriter;
import org.apache.sanselan.formats.tiff.TiffImageMetadata;
import org.apache.sanselan.formats.tiff.constants.TiffConstants;
import org.apache.sanselan.formats.tiff.write.TiffOutputSet;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.common.io.Files;

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

			if (outImageSize > 0) {

				Bitmap bitmap = BitmapFactory.decodeFile(inFile.getAbsolutePath());

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

				// rotate the image and then remove exif rotation info
//			      ExifInterface exif = new ExifInterface(inFile.getAbsolutePath());
//			            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
//			            Matrix matrix = new Matrix();
//			      if (orientation == 6) {
//			        matrix.postRotate(30);
//			      } else if (orientation == 3) {
//			        matrix.postRotate(30);
//			      } else if (orientation == 8) {
//			        matrix.postRotate(30);
//			      }
//			            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true); // rotating bitmap
//			      bitmap.compress(CompressFormat.JPEG, 100, out);
//			      
//			      // modify rotation exif
//			            exif.setAttribute(ExifInterface.TAG_ORIENTATION, "1");
//			            exif.saveAttributes();			
				
				OutputStream out = new FileOutputStream(stagedFile);
				bitmap.compress(CompressFormat.JPEG, 100, out);

				out.flush();
				out.close();
				bitmap.recycle();
				copyExifData(inFile, stagedFile);
			} else {
				Files.copy(inFile, stagedFile);
			}
		} else {
			Files.copy(inFile, stagedFile);
		}
		attachment.setLocalPath(stagedFile.getAbsolutePath());
		DaoStore.getInstance(context).getAttachmentDao().update(attachment);
	}

	public static void copyExifData(File sourceFile, File destFile) {
		String tempFileName = destFile.getAbsolutePath() + ".tmp";
		File tempFile = null;
		OutputStream tempStream = null;

		try {
			tempFile = new File(tempFileName);
			TiffOutputSet sourceSet = getSanselanOutputSet(sourceFile);

			// Save data to destination
			tempStream = new BufferedOutputStream(new FileOutputStream(tempFile));
			new ExifRewriter().updateExifMetadataLossless(destFile, tempStream, sourceSet);
			tempStream.close();

			if (destFile.delete()) {
				tempFile.renameTo(destFile);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (tempStream != null) {
				try {
					tempStream.close();
				} catch (IOException e) {
				}
			}

			if (tempFile != null) {
				if (tempFile.exists()) {
					tempFile.delete();
				}
			}
		}
	}

	private static TiffOutputSet getSanselanOutputSet(File jpegImageFile) throws Exception {
		TiffImageMetadata exif = null;
		TiffOutputSet outputSet = null;

		IImageMetadata metadata = Sanselan.getMetadata(jpegImageFile);
		JpegImageMetadata jpegMetadata = (JpegImageMetadata) metadata;
		if (jpegMetadata != null) {
			exif = jpegMetadata.getExif();

			if (exif != null) {
				outputSet = exif.getOutputSet();
			}
		}

		// If JPEG file contains no EXIF metadata, create an empty set of EXIF metadata. Otherwise, use existing EXIF metadata to keep all other existing tags
		if (outputSet == null) {
			outputSet = new TiffOutputSet(exif == null ? TiffConstants.DEFAULT_TIFF_BYTE_ORDER : exif.contents.header.byteOrder);
		}

		return outputSet;
	}
}
