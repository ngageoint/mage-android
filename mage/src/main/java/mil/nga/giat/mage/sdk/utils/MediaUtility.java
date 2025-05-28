package mil.nga.giat.mage.sdk.utils;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.google.common.io.ByteStreams;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.annotation.Nullable;

import javax.annotation.Nullable;

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

	public static File copyMediaFromUri(Context context, Uri uri) throws IOException {
		InputStream is = null;
		OutputStream os = null;
		try {
			ContentResolver contentResolver = context.getContentResolver();

			String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
			String imageFileName = "MAGE_" + timeStamp;

			String displayName = getDisplayName(context, uri);
			String extension = getFileExtension(displayName);
			if (extension == null) {
				MimeTypeMap mime = MimeTypeMap.getSingleton();
				extension = mime.getExtensionFromMimeType(contentResolver.getType(uri));
			}

			if (extension == null) {
				throw new IOException("Cannot determine file extension for file " + displayName);
			}

			File directory  = context.getExternalFilesDir("media");
			File file =  File.createTempFile(
					imageFileName,  /* prefix */
					"." + extension,         /* suffix */
					directory      /* directory */
			);

			is = contentResolver.openInputStream(uri);
			os = new FileOutputStream(file);
			ByteStreams.copy(is, os);

			return file;
		}  finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
				}
			}

			if (os != null) {
				try {
					os.close();
				} catch (IOException e) {
				}
			}
		}
	}

	public static File getMediaStageDirectory(Context context) {
		File directory = new File(context.getFilesDir(), "media");
		if (!directory.exists()) {
			directory.mkdirs();
		}

		return directory;
	}

	public static File getUserIconDirectory(Context context) {
		File directory = getMediaStageDirectory(context);
		File iconDirectory = new File(directory, "/user/icons");
		if (!iconDirectory.exists()) {
			iconDirectory.mkdirs();
		}

		return iconDirectory;
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
        return getColumn(context, uri, MediaStore.Files.FileColumns.DATA, selection, selectionArgs);
    }

    /**
     * Get the value of the display name for this Uri
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @param selection (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     * @author paulburke
     */
    public static String getDisplayNameColumn(Context context, Uri uri, String selection,
											  String[] selectionArgs) {
        return getColumn(context, uri, DocumentsContract.Document.COLUMN_DISPLAY_NAME, selection, selectionArgs);
    }

    /**
     * Get the value of the column for this Uri
     *
     * @param context The context.
     * @param column The column.
     * @param uri The Uri to query.
     * @param selection (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getColumn(Context context, Uri uri, String column, String selection,
								   String[] selectionArgs) {

        Cursor cursor = null;
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
        } catch (Exception e) {
            Log.w(LOG_NAME, "Error getting " + column + " column", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    /**
	 * From:
	 * https://github.com/iPaulPro/aFileChooser/blob/master/aFileChooser/src/com/ipaulpro/afilechooser/utils/FileUtils.java
	 *
	 * MODIFIED FOR MAGE:
	 *
	 * - Removed LocalStorageProvider references
	 * - Added and modified to use isDocumentUri and getDocumentId methods with KITKAT target api annotation
	 * - Added ExternalStorageProvider SD card handler section in the getPath method
	 * - Added getFileIfExists method
	 *
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.<br>
     * <br>
     * Callers should check whether the path is local before assuming it
     * represents a local file.
     * 
     * @param context The context.
     * @param uri The Uri to query.
	 * @author paulburke
     */
    public static @Nullable String getPath(final Context context, final Uri uri) {
        if (isDocumentUri(context, uri)) {

            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

				// Handle SD cards
				File file = getFileIfExists("/storage/extSdCard", split[1]);
				if(file != null){
					return file.getAbsolutePath();
				}
				file = getFileIfExists("/storage/sdcard1", split[1]);
				if(file != null){
					return file.getAbsolutePath();
				}
				file = getFileIfExists("/storage/usbcard1", split[1]);
				if(file != null){
					return file.getAbsolutePath();
				}
				file = getFileIfExists("/storage/sdcard0", split[1]);
				if(file != null){
					return file.getAbsolutePath();
				}

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = getDocumentId(uri);

                String rawPrefix = "raw:";
                if(id.startsWith(rawPrefix)){
                    return id.substring(rawPrefix.length());
                }

                try {
                    String[] contentUriPrefixesToTry = new String[]{
                            "content://downloads/public_downloads",
                            "content://downloads/my_downloads",
                            "content://downloads/all_downloads"
                    };


                    for (String contentUriPrefix : contentUriPrefixesToTry) {
                        Uri contentUri = ContentUris.withAppendedId(Uri.parse(contentUriPrefix), Long.valueOf(id));
                        try {
                            String path = getDataColumn(context, contentUri, null, null);
                            if (path != null) {
                                return path;
                            }
                        } catch (Exception e) {
                        }
                    }
                }catch(NumberFormatException e){
                }

                String displayName = getDisplayNameColumn(context, uri, null, null);
                if(displayName != null){
                    File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), displayName);
                    if(file.exists()){
                        return file.getAbsolutePath();
                    }
                }

            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = getDocumentId(uri);
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

	private static boolean isDocumentUri(Context context, Uri uri){
		return DocumentsContract.isDocumentUri(context, uri);
	}

	private static String getDocumentId(Uri documentUri){
		return DocumentsContract.getDocumentId(documentUri);
	}

	/**
	 * Attempt to get the file location from the base path and path if the file exists
	 * @param basePath
	 * @param path
	 * @return
	 */
	private static File getFileIfExists(String basePath, String path){
		File result = null;
		File file = new File(basePath);
		if(file.exists())
		{
			file = new File(file, path);
			if(file.exists()){
				result = file;
			}
		}
		return result;
	}

	/**
	 * Attempt to detect temporary file paths so that the files can be copied as needed
	 * @param path
	 * @return true if a temporary file path
	 */
	public static boolean isTemporaryPath(String path){
		boolean temporary = isGoogleDocsInternalPath(path);
		return temporary;
	}

	/**
	 * Determine if the file path is an internal Google docs / drive path
	 * @param path file path
	 * @return true if an internal path
	 */
	public static boolean isGoogleDocsInternalPath(String path){
		return path.contains("com.google.android.apps.docs/files/fileinternal");
	}

	/**
	 * Get display name from the uri
	 *
	 * @param context
	 * @param uri
	 * @return
	 */
	public static String getDisplayName(Context context, Uri uri) {
		String name = getDisplayNameColumn(context, uri, null, null);

		if (name == null) {
			name = uri.getPath();
			int index = name.lastIndexOf('/');
			if (index != -1) {
				name = name.substring(index + 1);
			}
		}

		return name;
	}

	/**
	 * Get the display name from the URI and path
	 *
	 * @param context
	 * @param uri
	 * @param path
	 * @return
	 */
	public static String getDisplayName(Context context, Uri uri, String path) {

		// Try to get the GeoPackage name
		String name = null;
		if (path != null) {
			name = new File(path).getName();
		} else {
			name = getDisplayName(context, uri);
		}

		return name;
	}

	/**
	 * Get the file extension
	 *
	 * @param name
	 * @return
	 */
	public static String getFileExtension(String name) {

		String extension = null;

		int extensionIndex = name.lastIndexOf(".");
		if (extensionIndex > -1) {
			extension = name.substring(extensionIndex + 1);
		}

		return extension;
	}

	/**
	 * Get the file name with the extension removed
	 *
	 * @param file
	 * @return
	 */
	public static String getFileNameWithoutExtension(File file) {

		String name = file.getName();
		name = getNameWithoutExtension(name);

		return name;
	}

	/**
	 * Get the name with the extension removed
	 *
	 * @param name
	 * @return
	 */
	public static String getNameWithoutExtension(String name) {

		int extensionIndex = name.lastIndexOf(".");
		if (extensionIndex > -1) {
			name = name.substring(0, extensionIndex);
		}

		return name;
	}

}
