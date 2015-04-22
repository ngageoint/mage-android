package mil.nga.giat.mage.sdk.utils;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.webkit.MimeTypeMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.sanselan.Sanselan;
import org.apache.sanselan.common.IImageMetadata;
import org.apache.sanselan.formats.jpeg.JpegImageMetadata;
import org.apache.sanselan.formats.jpeg.exifRewrite.ExifRewriter;
import org.apache.sanselan.formats.tiff.TiffImageMetadata;
import org.apache.sanselan.formats.tiff.constants.TiffConstants;
import org.apache.sanselan.formats.tiff.write.TiffOutputSet;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Some code below from openintents
 *
 * Copyright (C) 2007-2008 OpenIntents.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
public class MediaUtility {

	private static final String LOG_NAME = MediaUtility.class.getName();
	
	private static int getPowerOfTwoForSampleRatio(double ratio){
        int k = Integer.highestOneBit((int)Math.floor(ratio));
        if(k==0) return 1;
        else return k;
    }
	
	public static String getMimeType(String url) {
	    String type = null;
		if(StringUtils.isBlank(url)) {
			return type;
		}
		String extension = null;
		try {
			extension = MimeTypeMap.getFileExtensionFromUrl(URLEncoder.encode(url.replaceAll("\\s*", ""), "UTF-8"));
		} catch(UnsupportedEncodingException uee) {
			Log.e(LOG_NAME, "Unable to determine file extension");
		}

		if(StringUtils.isBlank(extension)) {
			int i = url.lastIndexOf('.');
			if (i > 0 && url.length() >= i+1) {
				extension = url.substring(i+1);
			}
		}

	    if (!StringUtils.isBlank(extension)) {
	        MimeTypeMap mime = MimeTypeMap.getSingleton();
	        type = mime.getMimeTypeFromExtension(extension);
	    }
	    return type;
	}
	
	public static Boolean isImage(String filePath) {
		String mime = getMimeType(filePath);
		if(mime == null) {
			return false;
		}
		return mime.toLowerCase().matches("image/.*");
	}
	
	public static void addImageToGallery(Context c, Uri contentUri) {
	    Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
	    mediaScanIntent.setData(contentUri);
	    c.sendBroadcast(mediaScanIntent);
	}
	
	public static File createImageFile() throws IOException {
	    // Create an image file name
	    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
	    String imageFileName = "MAGE_" + timeStamp;
	    File storageDir = Environment.getExternalStoragePublicDirectory(
	            Environment.DIRECTORY_PICTURES);
	    return File.createTempFile(
	        imageFileName,  /* prefix */
	        ".jpg",         /* suffix */
	        storageDir      /* directory */
	    );
	}
	
	public static File getMediaStageDirectory() {
		File sd = Environment.getExternalStorageDirectory();
		File mediaFolder = new File(sd, "/MAGE/Media");
		if (sd.canWrite()) {
			if (!mediaFolder.exists()) {
				mediaFolder.mkdirs();
			}
		}
		return mediaFolder;
	}
	
	public static File getAvatarDirectory() {
		File sd = Environment.getExternalStorageDirectory();
		File mediaFolder = new File(sd, "/MAGE/Media/user/avatars");
		if (sd.canWrite()) {
			if (!mediaFolder.exists()) {
				mediaFolder.mkdirs();
			}
		}
		return mediaFolder;
	}
	
	public static File getUserIconDirectory() {
		File sd = Environment.getExternalStorageDirectory();
		File mediaFolder = new File(sd, "/MAGE/Media/user/icons");
		if (sd.canWrite()) {
			if (!mediaFolder.exists()) {
				mediaFolder.mkdirs();
			}
		}
		return mediaFolder;
	}
	
    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @param selection (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection,
            String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {

                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.<br>
     * <br>
     * Callers should check whether the path is local before assuming it
     * represents a local file.
     * 
     * @param context The context.
     * @param uri The Uri to query.
     */
    public static String getPath(final Context context, final Uri uri) {


        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // LocalStorageProvider
//            if (isLocalStorageDocument(uri)) {
//                // The path is the id
//                return DocumentsContract.getDocumentId(uri);
//            }
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {

            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();

            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }
	
	public static String getFileAbsolutePath(Uri uri, Context c) 
	{
	    String fileName = null;
	    String scheme = uri.getScheme();
	    if (scheme.equals("file")) {
	        fileName = uri.getPath();
	    }
	    else if (scheme.equals("content")) {
	    	Cursor cursor = null;
	    	  try { 
	    	    String[] proj = { MediaStore.Images.Media.DATA };
	    	    cursor = c.getContentResolver().query(uri,  proj, null, null, null);
	    	    int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
	    	    cursor.moveToFirst();
	    	    return cursor.getString(column_index);
	    	  } finally {
	    	    if (cursor != null) {
	    	      cursor.close();
	    	    }
	    	  }
	    }
	    return fileName;
	}
	
	public static Bitmap getFullSizeOrientedBitmap(Uri uri, Context c) throws FileNotFoundException, IOException {
		InputStream is = c.getContentResolver().openInputStream(uri);
		return MediaUtility.orientBitmap(BitmapFactory.decodeStream(is, null, null), getFileAbsolutePath(uri, c), false);
	}
	
	public static Bitmap getThumbnailFromContent(Uri uri, int thumbsize, Context c) throws FileNotFoundException, IOException {
		InputStream is = c.getContentResolver().openInputStream(uri);
		return MediaUtility.getThumbnail(is, thumbsize, getFileAbsolutePath(uri, c));
	}
	
	public static Bitmap getThumbnail(File file, int thumbsize) throws FileNotFoundException, IOException {
		FileInputStream input = new FileInputStream(file);
		return MediaUtility.getThumbnail(input, thumbsize, file.getAbsolutePath());
    }
	
	// TODO: this will only allow thumbnails based on the max of width or height.  We should allow choosing either height or width.
	// Be aware that this method also rotates the image so height/width potentially could change and I think we should probably
	// not rotate until it is resized to save memory
	public static Bitmap getThumbnail(InputStream input, int thumbsize, String absoluteFilePath) throws FileNotFoundException, IOException {
		BitmapFactory.Options onlyBoundsOptions = new BitmapFactory.Options();
        onlyBoundsOptions.inJustDecodeBounds = true;
        onlyBoundsOptions.inDither=true;//optional
        onlyBoundsOptions.inPreferredConfig=Bitmap.Config.ARGB_8888;//optional
        BitmapFactory.decodeStream(input, null, onlyBoundsOptions);
        input.close();
        if ((onlyBoundsOptions.outWidth == -1) || (onlyBoundsOptions.outHeight == -1))
            return null;

        int originalSize = (onlyBoundsOptions.outHeight > onlyBoundsOptions.outWidth) ? onlyBoundsOptions.outHeight : onlyBoundsOptions.outWidth;

        double ratio = (originalSize > thumbsize) ? (originalSize / thumbsize) : 1.0;

        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
        bitmapOptions.inSampleSize = getPowerOfTwoForSampleRatio(ratio);
        bitmapOptions.inDither=true;//optional
        bitmapOptions.inPreferredConfig=Bitmap.Config.ARGB_8888;//optional
        input = new FileInputStream(absoluteFilePath);
        
        Bitmap bitmap = MediaUtility.orientBitmap(BitmapFactory.decodeStream(input, null, bitmapOptions), absoluteFilePath, false);
        input.close();
        return bitmap;
	}
	
	public static Bitmap getThumbnail(InputStream input, int thumbsize) throws IOException {
		BitmapFactory.Options onlyBoundsOptions = new BitmapFactory.Options();
        onlyBoundsOptions.inJustDecodeBounds = true;
        onlyBoundsOptions.inDither=true;//optional
        onlyBoundsOptions.inPreferredConfig=Bitmap.Config.ARGB_8888;//optional
        BitmapFactory.decodeStream(input, null, onlyBoundsOptions);
        input.close();
        if ((onlyBoundsOptions.outWidth == -1) || (onlyBoundsOptions.outHeight == -1))
            return null;

        int originalSize = (onlyBoundsOptions.outHeight > onlyBoundsOptions.outWidth) ? onlyBoundsOptions.outHeight : onlyBoundsOptions.outWidth;

        double ratio = (originalSize > thumbsize) ? (originalSize / thumbsize) : 1.0;

        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
        bitmapOptions.inSampleSize = getPowerOfTwoForSampleRatio(ratio);
        bitmapOptions.inDither=true;//optional
        bitmapOptions.inPreferredConfig=Bitmap.Config.ARGB_8888;//optional
        return BitmapFactory.decodeStream(input, null, bitmapOptions);
	}

	// FIXME : phone will run out of memory on big images...
	public static Bitmap orientBitmap(Bitmap bitmap, String absoluteFilePath, boolean setOrientationOnFile) throws IOException {
		// Rotate the picture based on the exif data
        ExifInterface exif = new ExifInterface(absoluteFilePath);
        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
        Matrix matrix = new Matrix();
        if (orientation == 6) {
            matrix.postRotate(90);
        } else if (orientation == 3) {
            matrix.postRotate(180);
        } else if (orientation == 8) {
            matrix.postRotate(270);
        }

        Bitmap newBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true); // rotating bitmap
		if(setOrientationOnFile) {
			exif.setAttribute(ExifInterface.TAG_ORIENTATION, "1");
			exif.saveAttributes();
		}
		return newBitmap;
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
	
	public static Bitmap resizeAndRoundCorners(Bitmap bitmap, int maxSize) {
		boolean isLandscape = bitmap.getWidth() > bitmap.getHeight();

        int newWidth, newHeight;
        if (isLandscape)
        {
            newWidth = maxSize;
            newHeight = Math.round(((float) newWidth / bitmap.getWidth()) * bitmap.getHeight());
        } else
        {
            newHeight = maxSize;
            newWidth = Math.round(((float) newHeight / bitmap.getHeight()) * bitmap.getWidth());
        }

        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, false);

        if (resizedBitmap != bitmap)
        	bitmap.recycle();
    	
        Bitmap roundedProfile = Bitmap.createBitmap(resizedBitmap.getWidth(), resizedBitmap
                .getHeight(), Config.ARGB_8888);
        
        Canvas roundedCanvas = new Canvas(roundedProfile);
        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, roundedProfile.getWidth(), roundedProfile.getHeight());
        final RectF rectF = new RectF(rect);
        final float roundPx = 7.0f;
        
        paint.setAntiAlias(true);
        roundedCanvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        roundedCanvas.drawRoundRect(rectF, roundPx, roundPx, paint);

        paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
        roundedCanvas.drawBitmap(resizedBitmap, rect, rect, paint);
        return roundedProfile;
	}

}
