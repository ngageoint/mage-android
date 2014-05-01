package mil.nga.giat.mage.observation;

import java.io.File;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.observation.RemoveAttachmentDialogFragment.RemoveAttachmentDialogListener;
import mil.nga.giat.mage.sdk.datastore.observation.Attachment;
import mil.nga.giat.mage.sdk.preferences.PreferenceHelper;
import mil.nga.giat.mage.sdk.utils.MediaUtility;
import android.content.Intent;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

public class AttachmentViewerActivity extends FragmentActivity implements RemoveAttachmentDialogListener {
	
	public static String EDITABLE = "EDITABLE";
	public static String ATTACHMENT = "ATTACHMENT";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.attachment_viewer);
		this.setTitle("Observation Attachment");
		
		
		Intent intent = getIntent();
		ImageView iv = (ImageView)findViewById(R.id.image);
		if (!intent.getBooleanExtra(EDITABLE, false)) {
			findViewById(R.id.remove_btn).setVisibility(View.GONE);
		}
		
		final Attachment a = intent.getParcelableExtra("attachment");
		
		String absPath = a.getLocalPath();
		String url = a.getUrl();
		
		// get content type from everywhere I can think of
		String contentType = a.getContentType();
		String name = null;
		if (contentType == null || "".equalsIgnoreCase(contentType) || "application/octet-stream".equalsIgnoreCase(contentType)) {
			name = a.getName();
			if (name == null) {
				name = a.getLocalPath();
				if (name == null) {
					name = a.getRemotePath();
				}
			}
			contentType = MediaUtility.getMimeType(name);
		}
		
		final String finalType;
		final Uri uri;
		
		if (absPath != null) {
			File f = new File(absPath);
			uri = Uri.fromFile(f);
			if (contentType.startsWith("image")) {
				finalType = "image/*";
				Glide.load(f).placeholder(android.R.drawable.progress_indeterminate_horizontal).centerCrop().into(iv);
			} else if (contentType.startsWith("video")) {
				finalType = "video/*";
				iv.setImageBitmap(ThumbnailUtils.createVideoThumbnail(absPath, MediaStore.Video.Thumbnails.FULL_SCREEN_KIND));
				findViewById(R.id.video_overlay_image).setVisibility(View.VISIBLE);
			} else if (contentType.startsWith("audio")) {
				finalType = "audio/*";
				Glide.load(R.drawable.ic_microphone).into(iv);
			} else {
				finalType = null;
			}
		} else if (url != null) {
			uri = Uri.parse(url);
			if (contentType.startsWith("image")) {
				finalType = "image/*";
				Glide.load(url).placeholder(android.R.drawable.progress_indeterminate_horizontal).centerCrop().into(iv);
			} else if (contentType.startsWith("video")) {
				finalType = "video/*";
				// TODO figure out how to set accepts to image/jpeg to ask the server for a thumbnail for the video
				Glide.load(R.drawable.ic_video_2x).into(iv);
				findViewById(R.id.video_overlay_image).setVisibility(View.VISIBLE);
			} else if (contentType.startsWith("audio")) {
				finalType = "audio/*";
				Glide.load(R.drawable.ic_microphone).into(iv);
			} else {
				finalType = null;
			}
		} else {
			uri = null;
			finalType = null;
		}
		if (uri != null && finalType != null) {
			iv.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Log.i("viewer", "launching viewer for " + uri + " type " + finalType);
					Intent intent = new Intent(Intent.ACTION_VIEW);
					intent.setDataAndType(uri, finalType);
					startActivity(intent);
				}
			});
		}
		
//
//		
//		
//		
//		
//
//		
//		Bitmap thumb = null;
//		
//
//    	if (absPath.endsWith(".mp4")) {
//    		Log.d("viewer", "abs path is: " + absPath + " uri is: " + imageUri);
//    		thumb = ThumbnailUtils.createVideoThumbnail(absPath, MediaStore.Video.Thumbnails.FULL_SCREEN_KIND);
//    		
//    		iv.setOnClickListener(new View.OnClickListener() {
//				
//				@Override
//				public void onClick(View v) {
//					Log.d("viewer", "launching viewer for " + imageUri);
//					Intent intent = new Intent(Intent.ACTION_VIEW);
//					intent.setDataAndType(imageUri, "video/*");
//					startActivity(intent);
//				}
//			});
//    	} else if (absPath.endsWith(".mp3") || absPath.endsWith(".m4a")) {
//    		thumb = BitmapFactory.decodeResource(getResources(), R.drawable.ic_microphone);
//    		
//    		iv.setOnClickListener(new View.OnClickListener() {
//				
//				@Override
//				public void onClick(View v) {
//					Intent intent = new Intent(Intent.ACTION_VIEW);
//					intent.setDataAndType(imageUri, "audio/*");
//					startActivity(intent);
//				}
//			});
//    	} else {
//			Display display = getWindowManager().getDefaultDisplay();
//			Point size = new Point();
//			display.getSize(size);
//			int height = size.y;
//			try {
//				thumb = MediaUtility.getThumbnailFromContent(imageUri, height, getApplicationContext());
//			} catch (Exception e) {
//				
//			}
//			findViewById(R.id.video_overlay_image).setVisibility(View.GONE);
//			iv.setOnClickListener(new View.OnClickListener() {
//				
//				@Override
//				public void onClick(View v) {
//					Intent intent = new Intent(Intent.ACTION_VIEW);
//					intent.setDataAndType(imageUri, "image/*");
//					startActivity(intent);
//				}
//			});
//    	}
//    	
//    	try {
//			iv.setImageBitmap(thumb);
//		} catch (Exception e) {
//			Log.e("Image viewer", "Error viewing image", e);
//		}
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
