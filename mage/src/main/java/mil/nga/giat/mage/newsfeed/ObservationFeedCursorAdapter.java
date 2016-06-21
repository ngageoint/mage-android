package mil.nga.giat.mage.newsfeed;

import android.content.Context;
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

import com.j256.ormlite.android.AndroidDatabaseResults;
import com.j256.ormlite.stmt.PreparedQuery;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.form.LayoutBaker;
import mil.nga.giat.mage.form.LayoutBaker.ControlGenerationType;
import mil.nga.giat.mage.map.marker.ObservationBitmapFactory;
import mil.nga.giat.mage.observation.AttachmentGallery;
import mil.nga.giat.mage.sdk.datastore.observation.Observation;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.datastore.user.UserHelper;
import mil.nga.giat.mage.sdk.exceptions.UserException;

public class ObservationFeedCursorAdapter extends CursorAdapter {

	private static final String LOG_NAME = ObservationFeedCursorAdapter.class.getName();
	
	private LayoutInflater inflater = null;
	private PreparedQuery<Observation> query;
    private AttachmentGallery attachmentGallery;

	public ObservationFeedCursorAdapter(Context context, Cursor c, PreparedQuery<Observation> query, AttachmentGallery attachmentGallery) {
		super(context, c, false);
		this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		this.query = query;
        this.attachmentGallery = attachmentGallery;
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
			Bitmap marker = ObservationBitmapFactory.bitmap(context, o);
			if (marker != null) {
				markerView.setImageBitmap(marker);
			}

			String user = "Unknown User";
			try {
				User u = UserHelper.getInstance(context).read(o.getUserId());
				if(u != null) {
					user = u.getDisplayName();
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
                attachmentGallery.addAttachments(attachmentLayout, o.getAttachments());
            }
		} catch (java.sql.SQLException e) {
			Log.e(LOG_NAME, "Problem getting observation.", e);
		}
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parentView) {
		View v = inflater.inflate(R.layout.observation_list_item, parentView, false);
		try {
			LayoutBaker.populateLayoutWithControls(
					(LinearLayout) v.findViewById(R.id.observation_list_dynamic_content),
					LayoutBaker.createControlsFromJson(v.getContext(), ControlGenerationType.VIEW, query.mapRow(new AndroidDatabaseResults(cursor, null)).getEvent().getForm()));
		} catch (java.sql.SQLException e) {
			Log.e(LOG_NAME, "Problem getting observation.", e);
		}
		return v;
	}
}