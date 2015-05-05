package mil.nga.giat.mage.observation;

import android.app.ProgressDialog;
import android.content.Intent;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.observation.RemoveAttachmentDialogFragment.RemoveAttachmentDialogListener;
import mil.nga.giat.mage.sdk.datastore.DaoStore;
import mil.nga.giat.mage.sdk.datastore.observation.Attachment;
import mil.nga.giat.mage.sdk.utils.MediaUtility;

public class AttachmentViewerActivity extends FragmentActivity implements RemoveAttachmentDialogListener {

	public final static String EDITABLE = "EDITABLE";
	public final static String ATTACHMENT = "ATTACHMENT";
	public final static String SHOULD_REMOVE = "SHOULD_REMOVE";
	public static final int DIALOG_DOWNLOAD_PROGRESS = 0;
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

		a = intent.getParcelableExtra(ATTACHMENT);

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
				Glide.with(getApplicationContext()).load(url).placeholder(android.R.drawable.progress_indeterminate_horizontal).centerCrop().into(iv);
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
						progressDialog.setMessage("Downloading file..");
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
		data.putExtra(ATTACHMENT, getIntent().getParcelableExtra(ATTACHMENT));
		setResult(RESULT_OK, data);
		finish();

	}

	@Override
	public void onDialogNegativeClick(DialogFragment dialog) {
	}

	private void startDownload(Attachment attachment, String mimeType) {
		String url = attachment.getUrl() + "?access_token=" + PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString(getString(mil.nga.giat.mage.sdk.R.string.tokenKey), null);
		new DownloadFileAsync(mimeType).execute(url);
	}

	class DownloadFileAsync extends AsyncTask<String, String, String> {
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
		protected String doInBackground(String... aurl) {
			int count;

			try {
				URL url = new URL(aurl[0]);
				URLConnection connection = url.openConnection();
				connection.connect();

				int lengthOfFile = connection.getContentLength();

				InputStream input = new BufferedInputStream(url.openStream());
				File stageDir = MediaUtility.getMediaStageDirectory();
				File stagedFile = new File(stageDir, a.getName());
				a.setLocalPath(stagedFile.getAbsolutePath());
				OutputStream output = new FileOutputStream(a.getLocalPath());

				byte data[] = new byte[1024];

				long total = 0;

				while ((count = input.read(data)) != -1) {
					total += count;
					publishProgress("" + (int) ((total * 100) / lengthOfFile));
					output.write(data, 0, count);
				}

				output.flush();
				output.close();
				input.close();
			} catch (Exception e) {
				Log.e(LOG_NAME, "Problem downloading file.");
			}
			return null;

		}

		protected void onProgressUpdate(String... progress) {
			progressDialog.setProgress(Integer.parseInt(progress[0]));
		}

		@Override
		protected void onPostExecute(String unused) {
			progressDialog.dismiss();
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
