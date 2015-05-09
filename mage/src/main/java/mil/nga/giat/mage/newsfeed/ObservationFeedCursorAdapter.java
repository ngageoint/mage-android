package mil.nga.giat.mage.newsfeed;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.j256.ormlite.android.AndroidDatabaseResults;
import com.j256.ormlite.stmt.PreparedQuery;

import java.io.File;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.form.LayoutBaker;
import mil.nga.giat.mage.form.LayoutBaker.ControlGenerationType;
import mil.nga.giat.mage.map.marker.ObservationBitmapFactory;
import mil.nga.giat.mage.observation.AttachmentViewerActivity;
import mil.nga.giat.mage.sdk.datastore.observation.Attachment;
import mil.nga.giat.mage.sdk.datastore.observation.Observation;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.datastore.user.UserHelper;
import mil.nga.giat.mage.sdk.exceptions.UserException;
import mil.nga.giat.mage.sdk.utils.MediaUtility;

public class ObservationFeedCursorAdapter extends CursorAdapter {

	private static final String LOG_NAME = ObservationFeedCursorAdapter.class.getName();
	
	private LayoutInflater inflater = null;
	private PreparedQuery<Observation> query;
	private Activity activity;

	public ObservationFeedCursorAdapter(Context context, Cursor c, PreparedQuery<Observation> query, Activity activity) {
		super(context, c, false);
		this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		this.activity = activity;
		this.query = query;
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public void bindView(View v, Context context, Cursor cursor) {
		try {
			Observation o = query.mapRow(new AndroidDatabaseResults(cursor, null));

			LayoutBaker.populateLayoutFromMap((LinearLayout) v.findViewById(R.id.observation_list_container), ControlGenerationType.VIEW, o.getPropertiesMap());

			ImageView markerView = (ImageView) v.findViewById(R.id.observation_marker);
			Bitmap marker = ObservationBitmapFactory.bitmap(activity, o);
			if (marker != null) {
				markerView.setImageBitmap(marker);
			}

			String user = "Unknown User";
			try {
				User u = UserHelper.getInstance(context).read(o.getUserId());
				if(u != null) {
					user = u.getFirstname() + " " + u.getLastname();
				}
			} catch (UserException e) {
				Log.e(LOG_NAME, "Could not get user", e);
			}

			((TextView) v.findViewById(R.id.username)).setText(user);

            LinearLayout attachmentLayout = (LinearLayout) v.findViewById(R.id.image_gallery);
            attachmentLayout.removeAllViews();
            if (o.getAttachments().size() == 0) {
                attachmentLayout.setVisibility(View.GONE);
            } else {
                attachmentLayout.setVisibility(View.VISIBLE);
                createImageViews(attachmentLayout, o);
            }
		} catch (java.sql.SQLException e) {
			Log.e(LOG_NAME, "Problem getting observation.", e);
		}
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parentView) {
		View v = inflater.inflate(R.layout.observation_list_item, parentView, false);
		try {
			LayoutBaker.populateLayoutWithControls((LinearLayout) v.findViewById(R.id.observation_list_dynamic_content), LayoutBaker.createControlsFromJson(v.getContext(), ControlGenerationType.VIEW, query.mapRow(new AndroidDatabaseResults(cursor, null)).getEvent().getForm()));
		} catch (java.sql.SQLException e) {
			Log.e(LOG_NAME, "Problem getting observation.", e);
		}
		return v;
	}

    private void createImageViews(ViewGroup gallery, Observation o) {
        for (final Attachment a : o.getAttachments()) {
            final String absPath = a.getLocalPath();
            final String remoteId = a.getRemoteId();
            ImageView iv = new ImageView(activity.getApplicationContext());
            ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(300, 300);
            iv.setLayoutParams(lp);
            iv.setPadding(0, 0, 10, 0);
            iv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(v.getContext(), AttachmentViewerActivity.class);
                    intent.putExtra(AttachmentViewerActivity.ATTACHMENT, a);
                    intent.putExtra(AttachmentViewerActivity.EDITABLE, false);
                    activity.startActivity(intent);
                }
            });
            gallery.addView(iv);

            // get content type from everywhere I can think of
            String contentType = a.getContentType();
            if (contentType == null || "".equalsIgnoreCase(contentType) || "application/octet-stream".equalsIgnoreCase(contentType)) {
                String name = a.getName();
                if (name == null) {
                    name = a.getLocalPath();
                    if (name == null) {
                        name = a.getRemotePath();
                    }
                }
                contentType = MediaUtility.getMimeType(name);
            }

            if (absPath != null) {
                if (contentType.startsWith("image")) {
                    Glide.with(activity.getApplicationContext()).load(new File(absPath)).placeholder(android.R.drawable.progress_indeterminate_horizontal).centerCrop().into(iv);
                } else if (contentType.startsWith("video")) {
                    Glide.with(activity.getApplicationContext()).load(R.drawable.ic_video_2x).into(iv);
                } else if (contentType.startsWith("audio")) {
                    Glide.with(activity.getApplicationContext()).load(R.drawable.ic_microphone).into(iv);
                }
            } else if (remoteId != null) {
                String url = a.getUrl();
                if (contentType.startsWith("image")) {
                    Glide.with(activity.getApplicationContext()).load(url).placeholder(android.R.drawable.progress_indeterminate_horizontal).centerCrop().into(iv);
                } else if (contentType.startsWith("video")) {
                    Glide.with(activity.getApplicationContext()).load(R.drawable.ic_video_2x).into(iv);
                } else if (contentType.startsWith("audio")) {
                    Glide.with(activity.getApplicationContext()).load(R.drawable.ic_microphone).into(iv);
                }
            }
        }
    }

}