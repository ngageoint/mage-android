package mil.nga.giat.mage.observation;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.sdk.utils.MediaUtils;
//import mil.nga.giat.mage.sdk.utils.MediaUtils;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.ImageView;
import android.widget.VideoView;

public class ImageViewerActivity extends Activity {
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.image_viewer);
		this.setTitle("Image Viewer");
		
		
		Intent intent = getIntent();
		final Uri imageUri = intent.getData();
		ImageView iv = (ImageView)findViewById(R.id.image);
		
		Bitmap thumb = null;
		
		String absPath = MediaUtils.getFileAbsolutePath(imageUri, getApplicationContext());

    	if (absPath.endsWith(".mp4")) {
    		
    		thumb = ThumbnailUtils.createVideoThumbnail(absPath, MediaStore.Video.Thumbnails.FULL_SCREEN_KIND);
    		
    		iv.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					Intent intent = new Intent(Intent.ACTION_VIEW, imageUri);
					startActivity(intent);
				}
			});
    	} else if (absPath.endsWith(".mp3") || absPath.endsWith(".m4a")) {
    		thumb = BitmapFactory.decodeResource(getResources(), R.drawable.ic_microphone);
    		findViewById(R.id.video_overlay_image).setVisibility(View.GONE);
    		iv.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					Intent intent = new Intent(Intent.ACTION_VIEW, imageUri);
					startActivity(intent);
				}
			});
    	} else {
			Display display = getWindowManager().getDefaultDisplay();
			Point size = new Point();
			display.getSize(size);
			int height = size.y;
			try {
				thumb = MediaUtils.getThumbnailFromContent(imageUri, height, getApplicationContext());
			} catch (Exception e) {
				
			}
			findViewById(R.id.video_overlay_image).setVisibility(View.GONE);
			
    	}
    	
    	try {
			iv.setImageBitmap(thumb);
		} catch (Exception e) {
			Log.e("Image viewer", "Error viewing image", e);
		}
	}
	
	@Override
	public void onDestroy() {
		ImageView imageView = (ImageView) findViewById(R.id.image);
		BitmapDrawable bd = (BitmapDrawable)imageView.getDrawable();
		bd.getBitmap().recycle();
		imageView.setImageBitmap(null);
		super.onDestroy();
	}
	
	public void removeImage(View v) {
		Log.d("Image viewer", "Remove the image");
	}
	
	public void goBack(View v) {
		Log.d("Image Viewer", "Go back");
		onBackPressed();
	}

}
