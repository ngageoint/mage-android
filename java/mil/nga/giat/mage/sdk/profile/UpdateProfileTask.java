package mil.nga.giat.mage.sdk.profile;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.SecureRandom;

import mil.nga.giat.mage.sdk.connectivity.ConnectivityUtility;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.http.post.MageServerPostRequests;
import mil.nga.giat.mage.sdk.utils.MediaUtility;

public class UpdateProfileTask extends AsyncTask<String, Void, User> {
	
	private User user;
	private Context context;
	
	private static final SecureRandom random = new SecureRandom();
	
	private static final String LOG_NAME = UpdateProfileTask.class.getName();
	
	public UpdateProfileTask(User user, Context context) {
		this.user = user;
		this.context = context;
	}


	@Override
	protected User doInBackground(String... params) {
		
		// get inputs
		String fileToUpload = params[0];
		
		// Make sure you have connectivity
		if (!ConnectivityUtility.isOnline(context)) {
			// TODO Auto-generated method stub
			return user;
		}
		
		File stageDir = MediaUtility.getMediaStageDirectory();
		File inFile = new File(fileToUpload);
		// add random string to the front of the filename to avoid conflicts
		File stagedFile = new File(stageDir, new BigInteger(30, random).toString(32) + new File(fileToUpload).getName());

		Log.d(LOG_NAME, "Staging file: " + stagedFile.getAbsolutePath());
		if (stagedFile.getAbsolutePath().equalsIgnoreCase(fileToUpload)) {
			Log.d(LOG_NAME, "Attachment is already staged.  Nothing to do.");
			return user;
		}
		
		Bitmap bitmap = MediaUtility.orientImage(inFile);

		// Scale file
		Integer inWidth = bitmap.getWidth();
		Integer inHeight = bitmap.getHeight();

		Integer outWidth = 2048;
		Integer outHeight = 2048;

		if (inWidth > inHeight) {
			outHeight = ((Double) ((inHeight.doubleValue() / inWidth.doubleValue()) * 2048.0)).intValue();
		} else if (inWidth < inHeight) {
			outWidth = ((Double) ((inWidth.doubleValue() / inHeight.doubleValue()) * 2048.0)).intValue();
		}
		bitmap = Bitmap.createScaledBitmap(bitmap, outWidth, outHeight, true);

		try {
			OutputStream out = new FileOutputStream(stagedFile);
			bitmap.compress(CompressFormat.JPEG, 100, out);

			out.flush();
			out.close();
			bitmap.recycle();
		} catch (Exception e) {
			Log.e(LOG_NAME, "failed to upload file", e);
		}
		
		Log.e(LOG_NAME, "Pushing new picture " + stagedFile);
		return MageServerPostRequests.postProfilePicture(user, stagedFile.getAbsolutePath(), context);
	}

}
