package mil.nga.giat.mage.observation;

import android.Manifest;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.glide.GlideApp;
import mil.nga.giat.mage.observation.RemoveAttachmentDialogFragment.RemoveAttachmentDialogListener;
import mil.nga.giat.mage.sdk.datastore.observation.Attachment;
import mil.nga.giat.mage.sdk.datastore.observation.AttachmentHelper;
import mil.nga.giat.mage.sdk.http.resource.ObservationResource;
import mil.nga.giat.mage.sdk.utils.MediaUtility;
import okhttp3.ResponseBody;

public class AttachmentViewerActivity extends AppCompatActivity implements RemoveAttachmentDialogListener {

	public final static String EDITABLE = "EDITABLE";
	public final static String ATTACHMENT_ID = "ATTACHMENT_ID";
	public final static String ATTACHMENT_PATH = "ATTACHMENT_PATH";
	public final static String SHOULD_REMOVE = "SHOULD_REMOVE";
	private static final String LOG_NAME = AttachmentViewerActivity.class.getName();

	private static final int PERMISSIONS_REQUEST_STORAGE = 100;

	private ProgressDialog progressDialog;
	private Attachment attachment;
	private String contentType;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.attachment_viewer);
		this.setTitle("Observation Attachment");

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		Intent intent = getIntent();
		ImageView iv = (ImageView) findViewById(R.id.image);
		if (!intent.getBooleanExtra(EDITABLE, false)) {
//			findViewById(R.id.remove_btn).setVisibility(View.GONE);
		}

		String url = null;
		String path = getIntent().getStringExtra(ATTACHMENT_PATH);

		if (path == null) {
			try {
				attachment = AttachmentHelper.getInstance(getApplicationContext()).read(getIntent().getLongExtra(ATTACHMENT_ID, 0L));
				path = attachment.getLocalPath();
				url = attachment.getUrl();
				contentType = attachment.getContentType();

				// get content type from everywhere I can think of
				if (StringUtils.isBlank(contentType) || "application/octet-stream".equalsIgnoreCase(contentType)) {
					String name = attachment.getName();
					if (name == null) {
						name = attachment.getLocalPath();
						if (name == null) {
							name = attachment.getRemotePath();
						}
					}
					contentType = MediaUtility.getMimeType(name);
				}

			} catch (Exception e) {
				Log.e(LOG_NAME, "Error getting attachment", e);
			}
		} else {
			contentType = MediaUtility.getMimeType(path);
		}

		if (path != null && new File(path).exists()) {
			Uri uri = Uri.fromFile(new File(path));
			if (contentType.startsWith("image")) {
				GlideApp.with(getApplicationContext()).load(uri).centerCrop().into(iv);
			} else if (contentType.startsWith("video")) {
				final VideoView videoView = (VideoView) findViewById(R.id.video);
				MediaController mediaController = new MediaController(this);
				mediaController.setAnchorView(videoView);
				videoView.setMediaController(mediaController);

				videoView.setVideoURI(uri);

				findViewById(R.id.video).setVisibility(View.VISIBLE);

				videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
					@Override
					public void onPrepared(MediaPlayer mp) {
						videoView.start();
					}
				});
			} else if (contentType.startsWith("audio")) {
				findViewById(R.id.audio_image).setVisibility(View.VISIBLE);

				final VideoView videoView = (VideoView) findViewById(R.id.video);
				final MediaController mediaController = new MediaController(this) {
					@Override
					public void hide() {
						//Do not hide.
					}
				};
				mediaController.setAnchorView(videoView);
				videoView.setMediaController(mediaController);
				videoView.setVideoURI(uri);

				findViewById(R.id.video).setVisibility(View.VISIBLE);
				videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
					@Override
					public void onPrepared(MediaPlayer mp) {
						videoView.start();
						mediaController.show();
					}
				});
			}
		} else if (url != null) {
			if (contentType.startsWith("image")) {
				Uri uri = Uri.parse(url);
				findViewById(R.id.progress).setVisibility(View.VISIBLE);
				GlideApp.with(getApplicationContext())
						.load(attachment)
						.listener(new RequestListener<Drawable>() {
							@Override
							public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
								findViewById(R.id.progress).setVisibility(View.INVISIBLE);
								Snackbar snackbar = Snackbar.make(findViewById(R.id.coordinator_layout), "Cannot download image, check connection.", Snackbar.LENGTH_LONG);
								snackbar.show();
								return false;
							}

							@Override
							public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
								findViewById(R.id.progress).setVisibility(View.INVISIBLE);
								return false;
							}
						})
						.into(iv);
			} else if (contentType.startsWith("video")) {
				findViewById(R.id.progress).setVisibility(View.VISIBLE);
				findViewById(R.id.video).setVisibility(View.VISIBLE);

				// TODO pass token in header
				String token = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString(getApplicationContext().getString(mil.nga.giat.mage.sdk.R.string.tokenKey), null);
				Uri uri = Uri.parse(url + "?access_token=" + token);

				final VideoView videoView = (VideoView) findViewById(R.id.video);
				MediaController mediaController = new MediaController(this);
				mediaController.setAnchorView(videoView);
				videoView.setMediaController(mediaController);


				videoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
					@Override
					public boolean onError(MediaPlayer mp, int what, int extra) {
						findViewById(R.id.video).setVisibility(View.INVISIBLE);
						findViewById(R.id.progress).setVisibility(View.INVISIBLE);
						Snackbar snackbar = Snackbar.make(findViewById(R.id.coordinator_layout), "Cannot play video, check connection.", Snackbar.LENGTH_LONG);
						snackbar.show();
						return true;
					}
				});

				videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
					@Override
					public void onPrepared(MediaPlayer mp) {
						findViewById(R.id.progress).setVisibility(View.INVISIBLE);
						videoView.start();
					}
				});

				videoView.setVideoURI(uri);
			} else if (contentType.startsWith("audio")) {
				findViewById(R.id.progress).setVisibility(View.VISIBLE);

				String token = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString(getApplicationContext().getString(mil.nga.giat.mage.sdk.R.string.tokenKey), null);
				Uri uri = Uri.parse(url + "?access_token=" + token);

				final VideoView videoView = (VideoView) findViewById(R.id.video);
				videoView.setBackground(ContextCompat.getDrawable(this, R.drawable.ic_mic_gray_48dp));
				videoView.setBackgroundColor(ContextCompat.getColor(this, android.R.color.white));
				final MediaController mediaController = new MediaController(this) {
					@Override
					public void hide() {
						//Do not hide.
					}
				};

				mediaController.setAnchorView(videoView);
				videoView.setMediaController(mediaController);
				findViewById(R.id.video).setVisibility(View.VISIBLE);

				videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
					@Override
					public void onPrepared(MediaPlayer mp) {
						findViewById(R.id.progress).setVisibility(View.INVISIBLE);
						findViewById(R.id.audio_image).setVisibility(View.VISIBLE);
						videoView.start();
						mediaController.show();
					}
				});

				videoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
					@Override
					public boolean onError(MediaPlayer mp, int what, int extra) {
						findViewById(R.id.progress).setVisibility(View.INVISIBLE);
						findViewById(R.id.video).setVisibility(View.INVISIBLE);
						Snackbar snackbar = Snackbar.make(findViewById(R.id.coordinator_layout), "Cannot play audio, check connection.", Snackbar.LENGTH_LONG);
						snackbar.show();
						return true;
					}
				});

				videoView.setVideoURI(uri);
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		if (attachment != null && attachment.getLocalPath() == null) {
			inflater.inflate(R.menu.attachment_save_menu, menu);
		}

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				this.finish();
				return true;
			case R.id.save:
				saveAttachment();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);

		switch (requestCode) {
			case PERMISSIONS_REQUEST_STORAGE: {
				if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					new DownloadFileAsync().execute(attachment);
				} else {
					if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
						new AlertDialog.Builder(this)
								.setTitle(R.string.storage_access_title)
								.setMessage(R.string.storage_access_message)
								.setPositiveButton(R.string.settings, new Dialog.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {
										Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
										intent.setData(Uri.fromParts("package", getApplicationContext().getPackageName(), null));
										startActivity(intent);
									}
								})
								.setNegativeButton(android.R.string.cancel, null)
								.show();
					}
				}

				break;
			}
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
		data.putExtra(ATTACHMENT_ID, attachment);
		setResult(RESULT_OK, data);
		finish();
	}

	@Override
	public void onDialogNegativeClick(DialogFragment dialog) {
	}

	private void saveAttachment() {
		if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(AttachmentViewerActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_STORAGE);
		} else {
			new DownloadFileAsync().execute(attachment);
		}
	}

	class DownloadFileAsync extends AsyncTask<Attachment, Integer, Boolean> {
		ProgressDialog progressDialog;

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			progressDialog = new ProgressDialog(AttachmentViewerActivity.this);
			progressDialog.setMessage("Saving file...");
			progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			progressDialog.setCancelable(false);
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

				Long contentLength = response.contentLength();

				String type;
				if (contentType.startsWith("image")) {
					type = Environment.DIRECTORY_PICTURES;
				} else if (contentType.startsWith("video")) {
					type = Environment.DIRECTORY_MOVIES;
				} else if (contentType.startsWith("audio")) {
					type = Environment.DIRECTORY_MUSIC;
				} else {
					type = Environment.DIRECTORY_DOWNLOADS;
				}

				File directory = MediaUtility.getPublicAttachmentsDirectory(type);
				File stagedFile = new File(directory, AttachmentViewerActivity.this.attachment.getName());
				AttachmentViewerActivity.this.attachment.setLocalPath(stagedFile.getAbsolutePath());
				os = new FileOutputStream(AttachmentViewerActivity.this.attachment.getLocalPath());

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
				Toast toast = Toast.makeText(getApplicationContext(), "Attachment Failed to Save", Toast.LENGTH_SHORT);
				toast.show();
				return;
			}

			try {
				AttachmentHelper attachmentHelper = AttachmentHelper.getInstance(getApplicationContext());
				Attachment attachment = attachmentHelper.read(AttachmentViewerActivity.this.attachment.getId());
				attachment.setLocalPath(AttachmentViewerActivity.this.attachment.getLocalPath());
				attachmentHelper.update(attachment);

				MediaUtility.addImageToGallery(getApplicationContext(), Uri.fromFile(new File(AttachmentViewerActivity.this.attachment.getLocalPath())));

				AttachmentViewerActivity.this.invalidateOptionsMenu();

				Toast toast = Toast.makeText(getApplicationContext(), "Attachment Successfully Saved", Toast.LENGTH_SHORT);
				toast.show();
			} catch (Exception e) {
				Log.e(LOG_NAME, "Error saving attachment to DB", e);
			}
		}
	}

}
