package mil.nga.giat.mage.observation;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.observation.RemoveAttachmentDialogFragment.RemoveAttachmentDialogListener;
import mil.nga.giat.mage.sdk.utils.MediaUtility;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.ImageView;

public class ImageViewerActivity extends FragmentActivity implements RemoveAttachmentDialogListener {
	
	public static String EDITABLE = "EDITABLE";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.image_viewer);
		this.setTitle("Observation Attachment");
		
		
		Intent intent = getIntent();
		final Uri imageUri = intent.getData();
		ImageView iv = (ImageView)findViewById(R.id.image);
		
		if (!intent.getBooleanExtra(EDITABLE, false)) {
			findViewById(R.id.remove_btn).setVisibility(View.GONE);
		}
		
		Bitmap thumb = null;
		
		String absPath = MediaUtility.getFileAbsolutePath(imageUri, getApplicationContext());

    	if (absPath.endsWith(".mp4")) {
    		Log.d("viewer", "abs path is: " + absPath + " uri is: " + imageUri);
    		thumb = ThumbnailUtils.createVideoThumbnail(absPath, MediaStore.Video.Thumbnails.FULL_SCREEN_KIND);
    		
    		iv.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					Log.d("viewer", "launching viewer for " + imageUri);
					Intent intent = new Intent(Intent.ACTION_VIEW);
					intent.setDataAndType(imageUri, "video/*");
					startActivity(intent);
				}
			});
    	} else if (absPath.endsWith(".mp3") || absPath.endsWith(".m4a")) {
    		thumb = BitmapFactory.decodeResource(getResources(), R.drawable.ic_microphone);
    		findViewById(R.id.video_overlay_image).setVisibility(View.GONE);
    		iv.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					Intent intent = new Intent(Intent.ACTION_VIEW);
					intent.setDataAndType(imageUri, "audio/*");
					startActivity(intent);
				}
			});
    	} else {
			Display display = getWindowManager().getDefaultDisplay();
			Point size = new Point();
			display.getSize(size);
			int height = size.y;
			try {
				thumb = MediaUtility.getThumbnailFromContent(imageUri, height, getApplicationContext());
			} catch (Exception e) {
				
			}
			findViewById(R.id.video_overlay_image).setVisibility(View.GONE);
			iv.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					Intent intent = new Intent(Intent.ACTION_VIEW);
					intent.setDataAndType(imageUri, "image/*");
					startActivity(intent);
				}
			});
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
		DialogFragment dialog = new RemoveAttachmentDialogFragment();
		dialog.show(getSupportFragmentManager(), "RemoveAttachmentDialogFragment");
	}
	
	public void goBack(View v) {
		Log.d("Image Viewer", "Go back");
		onBackPressed();
	}

	@Override
	public void onDialogPositiveClick(DialogFragment dialog) {
		Intent data = new Intent();
		data.setData(getIntent().getData());
		data.putExtra("REMOVE", true);
		setResult(RESULT_OK, data);
		finish();
		
	}

	@Override
	public void onDialogNegativeClick(DialogFragment dialog) {
	}

}
