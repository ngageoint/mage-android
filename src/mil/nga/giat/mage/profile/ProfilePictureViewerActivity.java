package mil.nga.giat.mage.profile;

import java.io.InputStream;

import mil.nga.giat.mage.R;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

public class ProfilePictureViewerActivity extends Activity {
	private static final String LOG_NAME = ProfilePictureViewerActivity.class.getName();
	
	public final static String IMAGE_URL = "IMAGE_URL";
	public final static String USER_FIRSTNAME = "USER_FIRSTNAME";
	public final static String USER_LASTNAME = "USER_LASTNAME";
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.attachment_viewer);
		Intent intent = getIntent();
		String firstname = intent.getStringExtra(USER_FIRSTNAME);
		String lastname = intent.getStringExtra(USER_LASTNAME);
		this.setTitle(firstname + " " + lastname);
		
		ImageView iv = (ImageView)findViewById(R.id.image);
		findViewById(R.id.remove_btn).setVisibility(View.GONE);
		
		final String avatarUrl = intent.getStringExtra(IMAGE_URL);
		new DownloadImageTask(iv).execute(avatarUrl);
	}
	
	public void goBack(View v) {
		onBackPressed();
	}
    
    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
	    ImageView bmImage;

	    public DownloadImageTask(ImageView bmImage) {
	        this.bmImage = bmImage;
	    }

	    protected Bitmap doInBackground(String... urls) {
	        String urldisplay = urls[0];
	        Bitmap mIcon11 = null;
	        try {
	            InputStream in = new java.net.URL(urldisplay).openStream();
	            mIcon11 = BitmapFactory.decodeStream(in);
	        } catch (Exception e) {
	            Log.e("Error", e.getMessage());
	            e.printStackTrace();
	        }
	        return mIcon11;
	    }

	    protected void onPostExecute(Bitmap bitmap) {
	        bmImage.setImageBitmap(bitmap);
	    }
	}
}
