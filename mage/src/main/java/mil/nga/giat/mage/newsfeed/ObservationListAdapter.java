package mil.nga.giat.mage.newsfeed;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.j256.ormlite.android.AndroidDatabaseResults;
import com.j256.ormlite.stmt.PreparedQuery;

import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.map.marker.ObservationBitmapFactory;
import mil.nga.giat.mage.observation.AttachmentGallery;
import mil.nga.giat.mage.observation.ObservationShapeStyleParser;
import mil.nga.giat.mage.sdk.datastore.observation.Observation;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationError;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationFavorite;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationHelper;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationProperty;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.datastore.user.UserHelper;
import mil.nga.giat.mage.sdk.exceptions.ObservationException;
import mil.nga.giat.mage.sdk.exceptions.UserException;
import mil.nga.wkb.geom.GeometryType;

/**
 * Created by wnewman on 10/30/17.
 */

public class ObservationListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface ObservationActionListener {
        void onObservationClick(Observation observation);
        void onObservationDirections(Observation observation);
    }

    private static final String LOG_NAME = ObservationListAdapter.class.getName();

    private static final String SHORT_TIME_PATTERN = "h:mm a";
    private static final String SHORT_DATE_PATTERN = "MMM d";

    private static final int TYPE_OBSERVATION = 1;
    private static final int TYPE_FOOTER = 2;

    private Context context;
    private Cursor cursor;
    private PreparedQuery<Observation> query;
    private AttachmentGallery attachmentGallery;
    private User currentUser;
    private ObservationActionListener observationActionListener;
    private String footerText;

    private class ObservationViewHolder extends RecyclerView.ViewHolder {
        private ImageView markerView;
        private ImageView shapeView;
        private TextView primaryView;
        private TextView timeView;
        private TextView secondaryView;
        private TextView userView;
        private View flaggedBadge;
        private View syncBadge;
        private View errorBadge;
        private LinearLayout attachmentLayout;
        private View favoriteView;
        private View directionsView;

        ObservationViewHolder(View view) {
            super(view);

            markerView = (ImageView) view.findViewById(R.id.observation_marker);
            shapeView = (ImageView) view.findViewById(R.id.observation_shape);
            primaryView = (TextView) view.findViewById(R.id.primary);
            timeView = (TextView) view.findViewById(R.id.time);
            secondaryView = (TextView) view.findViewById(R.id.secondary);
            userView = (TextView) view.findViewById(R.id.user);
            flaggedBadge = view.findViewById(R.id.flagged);
            syncBadge = view.findViewById(R.id.sync_status);
            errorBadge = view.findViewById(R.id.error_status);
            attachmentLayout = (LinearLayout) view.findViewById(R.id.image_gallery);
            favoriteView = view.findViewById(R.id.favorite);
            directionsView = view.findViewById(R.id.directions);
        }

        public void bind(final Observation observation, final ObservationActionListener listener) {
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    listener.onObservationClick(observation);
                }
            });
        }
    }

    private class FooterViewHolder extends RecyclerView.ViewHolder {
        private TextView footerText;

        FooterViewHolder(View view) {
            super(view);

            footerText = (TextView) view.findViewById(R.id.footer_text);
        }
    }

    public ObservationListAdapter(Context context, AttachmentGallery attachmentGallery, ObservationActionListener observationActionListener) {
        this.context = context;
        this.attachmentGallery = attachmentGallery;
        this.observationActionListener = observationActionListener;
    }

    public void setCursor(Cursor cursor, PreparedQuery<Observation> query) {
        this.cursor = cursor;
        this.query = query;
        this.notifyDataSetChanged();
    }

    public void setFooterText(String footerText) {
        this.footerText = footerText;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == cursor.getCount()) {
            return TYPE_FOOTER;
        } else {
            return TYPE_OBSERVATION;
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_OBSERVATION) {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.observation_list_item, parent, false);
            return new ObservationViewHolder(itemView);
        } else if (viewType == TYPE_FOOTER) {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.feed_footer, parent, false);
            return new FooterViewHolder(itemView);
        } else {
            return null;
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ObservationViewHolder) {
            bindObservationViewHolder(holder, position);
        } else if (holder instanceof FooterViewHolder) {
            bindFooterViewHolder(holder);
        }
    }

    @Override
    public int getItemCount() {
        return cursor.getCount() + 1;
    }

    private void bindObservationViewHolder(RecyclerView.ViewHolder holder, int position) {
        cursor.moveToPosition(position);
        ObservationViewHolder vh = (ObservationViewHolder) holder;

        try {

            final Observation observation = query.mapRow(new AndroidDatabaseResults(cursor, null, false));
            vh.bind(observation, observationActionListener);

            Bitmap marker = ObservationBitmapFactory.bitmap(context, observation);
            if (marker != null) {
                vh.markerView.setImageBitmap(marker);
            }

            Drawable drawable;
            if (observation.getGeometry().getGeometryType() == GeometryType.POINT) {
                drawable = ResourcesCompat.getDrawable(context.getResources(), R.drawable.ic_place_black_24dp, null);

                vh.shapeView.setImageDrawable(drawable);
                vh.shapeView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                DrawableCompat.setTint(drawable, Color.argb(138, 256, 256, 256));
            } else {
                drawable = observation.getGeometry().getGeometryType() == GeometryType.LINESTRING ?
                        ResourcesCompat.getDrawable(context.getResources(), R.drawable.line_string_marker, null) :
                        ResourcesCompat.getDrawable(context.getResources(), R.drawable.polygon_marker, null);

                vh.shapeView.setImageDrawable(drawable);
                vh.shapeView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                int strokeColor = ObservationShapeStyleParser.getStyle(context, observation).getStrokeColor();
                DrawableCompat.setTint(drawable, strokeColor);
            }

            ObservationProperty primary = observation.getPrimaryField();
            if (primary == null || primary.isEmpty()) {
                vh.primaryView.setVisibility(View.GONE);
            } else {
                vh.primaryView.setText(primary.getValue().toString());
                vh.primaryView.setVisibility(View.VISIBLE);
            }
            vh.primaryView.requestLayout();

            ObservationProperty secondary = observation.getSecondaryField();
            if (secondary == null || secondary.isEmpty()) {
                vh.secondaryView.setVisibility(View.GONE);
            } else {
                vh.secondaryView.setVisibility(View.VISIBLE);
                vh.secondaryView.setText(secondary.getValue().toString());
            }

            Date timestamp = observation.getTimestamp();
            String pattern = DateUtils.isToday(timestamp.getTime()) ? SHORT_TIME_PATTERN : SHORT_DATE_PATTERN;
            DateFormat dateFormat = new SimpleDateFormat(pattern, Locale.getDefault());
            vh.timeView.setText(dateFormat.format(timestamp));

            String userDisplayName = "Unknown User";
            try {
                User user = UserHelper.getInstance(context).read(observation.getUserId());
                if (user != null) {
                    userDisplayName = user.getDisplayName();
                }
            } catch (UserException e) {
                Log.e(LOG_NAME, "Could not get user", e);
            }

            vh.userView.setText(userDisplayName);

            boolean isFlagged = observation.getImportant() != null && observation.getImportant().isImportant();
            vh.flaggedBadge.setVisibility(isFlagged ? View.VISIBLE : View.GONE);

            ObservationError error = observation.getError();
            if (error != null) {
                boolean hasValidationError = error.getStatusCode() != null;
                vh.syncBadge.setVisibility(hasValidationError ? View.GONE : View.VISIBLE);
                vh.errorBadge.setVisibility(hasValidationError ? View.VISIBLE : View.GONE);
            } else {
                vh.syncBadge.setVisibility(View.GONE);
                vh.errorBadge.setVisibility(View.GONE);
            }

            vh.attachmentLayout.removeAllViews();
            if (observation.getAttachments().size() == 0) {
                vh.attachmentLayout.setVisibility(View.GONE);
            } else {
                vh.attachmentLayout.setVisibility(View.VISIBLE);
                attachmentGallery.addAttachments(vh.attachmentLayout, observation.getAttachments());
            }

            vh.favoriteView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggleFavorite(observation, v);
                }
            });
            setFavoriteImage(observation.getFavorites(), vh.favoriteView, isFavorite(observation));

            vh.directionsView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getDirections(observation);
                }
            });


        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void bindFooterViewHolder(RecyclerView.ViewHolder holder) {
        FooterViewHolder vh = (FooterViewHolder) holder;

        vh.footerText.setText(footerText);
    }

    private void setFavoriteImage(Collection<ObservationFavorite> favorites, View view, boolean isFavorite) {
        ImageView favoriteIcon = (ImageView) view.findViewById(R.id.favoriteIcon);
        if (isFavorite) {
            favoriteIcon.setColorFilter(ContextCompat.getColor(context, R.color.observation_favorite_active));
        } else {
            favoriteIcon.setColorFilter(ContextCompat.getColor(context, R.color.observation_favorite_inactive));
        }

        TextView favoriteCount = (TextView) view.findViewById(R.id.favoriteCount);
        favoriteCount.setVisibility(favorites.size() > 0 ? View.VISIBLE : View.GONE);
        favoriteCount.setText(Integer.toString(favorites.size()));
    }

    private void toggleFavorite(Observation observation, View view) {
        ObservationHelper observationHelper = ObservationHelper.getInstance(context);
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
            currentUser = UserHelper.getInstance(context).readCurrentUser();
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
