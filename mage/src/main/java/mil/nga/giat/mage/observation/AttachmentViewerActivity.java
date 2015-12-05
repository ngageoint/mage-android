package mil.nga.giat.mage.observation;

import android.app.ProgressDialog;
import android.content.Intent;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.squareup.okhttp.ResponseBody;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.observation.RemoveAttachmentDialogFragment.RemoveAttachmentDialogListener;
import mil.nga.giat.mage.sdk.datastore.DaoStore;
import mil.nga.giat.mage.sdk.datastore.observation.Attachment;
import mil.nga.giat.mage.sdk.datastore.observation.AttachmentHelper;
import mil.nga.giat.mage.sdk.http.resource.ObservationResource;
import mil.nga.giat.mage.sdk.utils.MediaUtility;

public class AttachmentViewerActivity extends FragmentActivity implements RemoveAttachmentDialogListener {

	public final static String EDITABLE = "EDITABLE";
	public final static String ATTACHMENT_ID = "ATTACHMENT_ID";
	public final static String SHOULD_REMOVE = "SHOULD_REMOVE";
	private static final String LOG_NAME = AttachmentViewerActivity.class.getName();
	private ProgressDialog progressDialog;
	private Attachment a;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.attachment_viewer);
		this.setTitle("Observation Attachment");

		Intent intent = getIntent();
		ImageView iv = (ImageView) findViewById(R.id.image);
		if (!intent.getBooleanExtra(EDITABLE, false)) {
			findViewById(R.id.remove_btn).setVisibility(View.GONE);
		}

		try {
			a = AttachmentHelper.getInstance(getApplicationContext()).read(getIntent().getLongExtra(ATTACHMENT_ID, 0L));
		} catch (Exception e) {
			Log.e(LOG_NAME, "Error getting attachment", e);
		}

		String absPath = a.getLocalPath();
		String url = a.getUrl();

		// get content type from everywhere I can think of
		String contentType = a.getContentType();
		String name = null;
		if (StringUtils.isBlank(contentType) || "application/octet-stream".equalsIgnoreCase(contentType)) {
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
				Glide.with(getApplicationContext()).load(f).placeholder(android.R.drawable.progress_indeterminate_horizontal).centerCrop().into(iv);
			} else if (contentType.startsWith("video")) {
				finalType = "video/*";
				iv.setImageBitmap(ThumbnailUtils.createVideoThumbnail(absPath, MediaStore.Video.Thumbnails.FULL_SCREEN_KIND));
				findViewById(R.id.video_overlay_image).setVisibility(View.VISIBLE);
			} else if (contentType.startsWith("audio")) {
				finalType = "audio/*";
				Glide.with(getApplicationContext()).load(R.drawable.ic_microphone).into(iv);
			} else {
				finalType = null;
			}
		} else if (url != null) {
			uri = Uri.parse(url);
			if (contentType.startsWith("image")) {
				finalType = "image/*";
				Glide.with(getApplicationContext()).load(a).placeholder(android.R.drawable.progress_indeterminate_horizontal).centerCrop().into(iv);
			} else if (contentType.startsWith("video")) {
				finalType = "video/*";
				// TODO figure out how to set accepts to image/jpeg to ask the server for a thumbnail for the video
				Glide.with(getApplicationContext()).load(R.drawable.ic_video_2x).into(iv);
				findViewById(R.id.video_overlay_image).setVisibility(View.VISIBLE);
			} else if (contentType.startsWith("audio")) {
				finalType = "audio/*";
				Glide.with(getApplicationContext()).load(R.drawable.ic_microphone).into(iv);
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
					if (a.getLocalPath() == null) {
						progressDialog = new ProgressDialog(AttachmentViewerActivity.this);
						progressDialog.setMessage("Downloading file...");
						progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
						progressDialog.setCancelable(false);
						startDownload(a, finalType);
					} else {
						File f = new File(a.getLocalPath());
						Uri uri = Uri.fromFile(f);
						Log.i(LOG_NAME, "launching viewer for " + uri + " type " + finalType);
						Intent intent = new Intent(Intent.ACTION_VIEW);
						intent.setDataAndType(uri, finalType);
						startActivity(intent);
					}
				}
			});
		}
	}

	public void removeImage(View v) {
		DialogFragment dialog = new RemoveAttachmentDialogFragment();
		dialog.show(getSupportFragmentManager(), "RemoveAttachmentDialogFragment");
	}

	public void goBack(View v) {
		onBackPressed();
	}

	@Override
	public void onDialogPositiveClick(DialogFragment dialog) {
		Intent data = new Intent();
		data.putExtra(SHOULD_REMOVE, true);
		data.putExtra(ATTACHMENT_ID, a.getId());
		setResult(RESULT_OK, data);
		finish();
	}

	@Override
	public void onDialogNegativeClick(DialogFragment dialog) {
	}

	private void startDownload(Attachment attachment, String mimeType) {
		new DownloadFileAsync(mimeType).execute(attachment);
	}

	class DownloadFileAsync extends AsyncTask<Attachment, Integer, Boolean> {
		String mimeType;

		public DownloadFileAsync(String mimeType) {
			this.mimeType = mimeType;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			progressDialog.show();
		}

		@Override
		protected Boolean doInBackground(Attachment... attachments) {
			InputStream is = null;
			OutputStream os = null;
			try {
				Attachment attachment = attachments[0];

				ObservationResource observationResource = new ObservationResource(getApplicationContext());
				ResponseBody response = observationResource.getAttachment(attachment);

				// FIXME : I'm not sure this works
				Long contentLength = response.contentLength();

				File stageDir = MediaUtility.getMediaStageDirectory();
				File stagedFile = new File(stageDir, a.getName());
				a.setLocalPath(stagedFile.getAbsolutePath());
				os = new FileOutputStream(a.getLocalPath());

				byte data[] = new byte[1024];

				Long total = 0l;
				int count;
				is = response.byteStream();
				while ((count = is.read(data)) != -1) {
					total += count;
					publishProgress(((Double)(100.0*(total.doubleValue()/contentLength.doubleValue()))).intValue());
					os.write(data, 0, count);
				}
			} catch (Exception e) {
				Log.e(LOG_NAME, "Problem downloading file.", e);
				return false;
			} finally {
				if (is != null) {
					try {
						is.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}

				if (os != null) {
					try {
						os.flush();
						os.close();;
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}

			return true;
		}

		protected void onProgressUpdate(Integer... progress) {
			progressDialog.setProgress(progress[0]);
		}

		@Override
		protected void onPostExecute(Boolean success) {
			progressDialog.dismiss();

			if (!success) {
				// TODO pop up dialog
				return;
			}

			try {
				Attachment attachment = DaoStore.getInstance(getApplicationContext()).getAttachmentDao().queryForId(a.getId());
				attachment.setLocalPath(a.getLocalPath());
				DaoStore.getInstance(getApplicationContext()).getAttachmentDao().update(attachment);
			} catch (Exception e) {
				Log.e(LOG_NAME, "Error saving attachment to DB", e);
			}
			File f = new File(a.getLocalPath());
			Uri uri = Uri.fromFile(f);
			Log.i(LOG_NAME, "launching viewer for " + uri + " type " + mimeType);
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setDataAndType(uri, mimeType);
			startActivity(intent);
		}
	}

}
