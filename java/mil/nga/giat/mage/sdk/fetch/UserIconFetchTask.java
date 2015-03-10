package mil.nga.giat.mage.sdk.fetch;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import mil.nga.giat.mage.sdk.R;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.datastore.user.UserHelper;
import mil.nga.giat.mage.sdk.utils.MediaUtility;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

public class UserIconFetchTask extends AsyncTask<User, Void, Void> {
	private static final String LOG_NAME = UserIconFetchTask.class.getName();
	
    Context context;

    public UserIconFetchTask(Context context) {
        this.context = context;
    }

    protected Void doInBackground(User... users) {
    	String token = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.tokenKey), null);
    	for (User user : users) {
    		Log.d(LOG_NAME, "Fetching icon at url: " + user.getIconUrl());
	        String urldisplay = user.getIconUrl() + "?access_token=" + token;
	        
	        try {
	            InputStream in = new java.net.URL(urldisplay).openStream();
	            Bitmap bitmap = BitmapFactory.decodeStream(in);
	    		
	    		FileOutputStream out = null;
	    		try {
	    			String localPath = MediaUtility.getUserIconDirectory() + "/" + user.getId();
	    		    out = new FileOutputStream(localPath);
	    		    bitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
	    		    user.setLocalIconPath(localPath);
	    		    UserHelper.getInstance(context).update(user);
	    		} catch (Exception e) {
		            Log.e(LOG_NAME, e.getMessage(), e);
	    		} finally {
	    		    try {
	    		        if (out != null) {
	    		            out.close();
	    		        }
	    		    } catch (IOException e) {
	    		        e.printStackTrace();
	    		    }
	    		}
	        } catch (Exception e) {
	            Log.e(LOG_NAME, e.getMessage(), e);
	        }
    	}
    	return null;
    }

}
