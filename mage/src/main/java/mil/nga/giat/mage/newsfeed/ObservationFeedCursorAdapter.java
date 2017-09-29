package mil.nga.giat.mage.newsfeed;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.CursorAdapter;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.gson.JsonElement;
import com.j256.ormlite.android.AndroidDatabaseResults;
import com.j256.ormlite.stmt.PreparedQuery;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.map.marker.ObservationBitmapFactory;
import mil.nga.giat.mage.observation.AttachmentGallery;
import mil.nga.giat.mage.sdk.datastore.observation.Observation;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationError;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationFavorite;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationHelper;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationProperty;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.datastore.user.UserHelper;
import mil.nga.giat.mage.sdk.exceptions.ObservationException;
import mil.nga.giat.mage.sdk.exceptions.UserException;

public class ObservationFeedCursorAdapter extends CursorAdapter {

	public interface ObservationActionListener {
		void onObservationDirections(Observation observation);
	}

	private static final String LOG_NAME = ObservationFeedCursorAdapter.class.getName();
	private static final String TYPE_PROPERTY_KEY= "type";
	private static final String SHORT_TIME_PATTERN = "h:mm a";
	private static final String SHORT_DATE_PATTERN = "MMM d";

	private LayoutInflater inflater = null;
	private PreparedQuery<Observation> query;
    private AttachmentGallery attachmentGallery;
	private User currentUser;
	private ObservationActionListener observationActionListener;

	public ObservationFeedCursorAdapter(Activity activity, Cursor c, PreparedQuery<Observation> query, AttachmentGallery attachmentGallery) {
		super(activity, c, false);

		this.inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		this.query = query;
        this.attachmentGallery = attachmentGallery;
	}

	public void setObservationShareListener(ObservationActionListener observationActionListener) {
		this.observationActionListener = observationActionListener;
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parentView) {
		return inflater.inflate(R.layout.observation_list_item, parentView, false);
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public void bindView(View v, Context context, Cursor cursor) {
		try {
			final Observation observation = query.mapRow(new AndroidDatabaseResults(cursor, null, false));

			boolean isFlagged = observation.getImportant() != null && observation.getImportant().isImportant();
			v.findViewById(R.id.flagged).setVisibility(isFlagged ? View.VISIBLE : View.GONE);

			ObservationError error = observation.getError();
			if (error != null) {
				boolean hasValidationError = error.getStatusCode() != null;
				v.findViewById(R.id.sync_status).setVisibility(hasValidationError ? View.GONE : View.VISIBLE);
				v.findViewById(R.id.error_status).setVisibility(hasValidationError ? View.VISIBLE : View.GONE);
			} else {
				v.findViewById(R.id.sync_status).setVisibility(View.GONE);
				v.findViewById(R.id.error_status).setVisibility(View.GONE);
			}

			ImageView markerView = (ImageView) v.findViewById(R.id.observation_marker);
			Bitmap marker = ObservationBitmapFactory.bitmap(context, observation);
			if (marker != null) {
				markerView.setImageBitmap(marker);
			}

			Map<String, ObservationProperty> properties = observation.getPropertiesMap();
			ObservationProperty type = properties.get(TYPE_PROPERTY_KEY);
			((TextView) v.findViewById(R.id.type)).setText(type.getValue().toString());

			JsonElement variantField = observation.getEvent().getForm().get("variantField");
			if (variantField != null && !variantField.isJsonNull()) {
				TextView variantTextView = ((TextView) v.findViewById(R.id.variant));
				ObservationProperty variant = properties.get(variantField.getAsString());

				if (variant != null && variant.getValue() != null) {
					variantTextView.setVisibility(View.VISIBLE);
					variantTextView.setText(variant.getValue().toString());
				} else {
					v.findViewById(R.id.variant).setVisibility(View.GONE);
				}
			} else {
				v.findViewById(R.id.variant).setVisibility(View.GONE);
			}

			Date timestamp = observation.getTimestamp();
			String pattern = DateUtils.isToday(timestamp.getTime()) ? SHORT_TIME_PATTERN : SHORT_DATE_PATTERN;
			DateFormat dateFormat = new SimpleDateFormat(pattern, Locale.getDefault());
			((TextView) v.findViewById(R.id.time)).setText(dateFormat.format(timestamp));

			String userDisplayName = "Unknown User";
			try {
				User user = UserHelper.getInstance(context).read(observation.getUserId());
				if (user != null) {
					userDisplayName = user.getDisplayName();
				}
			} catch (UserException e) {
				Log.e(LOG_NAME, "Could not get user", e);
			}

			((TextView) v.findViewById(R.id.user)).setText(userDisplayName);

			LinearLayout attachmentLayout = (LinearLayout) v.findViewById(R.id.image_gallery);
            attachmentLayout.removeAllViews();
            if (observation.getAttachments().size() == 0) {
                attachmentLayout.setVisibility(View.GONE);
            } else {
                attachmentLayout.setVisibility(View.VISIBLE);
                attachmentGallery.addAttachments(attachmentLayout, observation.getAttachments());
            }

			View favorite = v.findViewById(R.id.favorite);
			favorite.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					toggleFavorite(observation, v);
				}
			});
			setFavoriteImage(observation.getFavorites(), favorite, isFavorite(observation));

			v.findViewById(R.id.directions).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					getDirections(observation);
				}
			});

		} catch (java.sql.SQLException e) {
			Log.e(LOG_NAME, "Problem getting observation.", e);
		}
	}

	private void setFavoriteImage(Collection<ObservationFavorite> favorites, View view, boolean isFavorite) {
		ImageView favoriteIcon = (ImageView) view.findViewById(R.id.favoriteIcon);
		if (isFavorite) {
			favoriteIcon.setColorFilter(ContextCompat.getColor(mContext, R.color.observation_favorite_active));
		} else {
			favoriteIcon.setColorFilter(ContextCompat.getColor(mContext, R.color.observation_favorite_inactive));
		}

		TextView favoriteCount = (TextView) view.findViewById(R.id.favoriteCount);
		favoriteCount.setVisibility(favorites.size() > 0 ? View.VISIBLE : View.GONE);
		favoriteCount.setText(Integer.toString(favorites.size()));
	}

	private void toggleFavorite(Observation observation, View view) {
		ObservationHelper observationHelper = ObservationHelper.getInstance(mContext);
		boolean isFavorite = isFavorite(observation);
		try {
			if (isFavorite) {
				observationHelper.unfavoriteObservation(observation, currentUser);
			} else {
				observationHelper.favoriteObservation(observation, currentUser);
			}

			setFavoriteImage(observation.getFavorites(), view, isFavorite);
		} catch (ObservationException e) {
			Log.e(LOG_NAME, "Could not unfavorite observation", e);
		}
	}

	private boolean isFavorite(Observation observation) {
		boolean isFavorite = false;
		try {
			currentUser = UserHelper.getInstance(mContext).readCurrentUser();
			if (currentUser != null) {
				ObservationFavorite favorite = observation.getFavoritesMap().get(currentUser.getRemoteId());
				isFavorite = favorite != null && favorite.isFavorite();
			}
		} catch (UserException e) {
			Log.e(LOG_NAME, "Could not get user", e);
		}

		return isFavorite;
	}

	private void getDirections(Observation observation) {
		if (observationActionListener != null) {
			observationActionListener.onObservationDirections(observation);
		}
	}
}