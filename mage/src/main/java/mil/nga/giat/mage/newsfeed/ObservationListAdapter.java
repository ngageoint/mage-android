package mil.nga.giat.mage.newsfeed;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
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

import java.lang.ref.WeakReference;
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
        private IconTask iconTask;
        private UserTask userTask;
        private PropertyTask primaryPropertyTask;
        private PropertyTask secondaryPropertyTask;

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

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder holder) {
        if (holder instanceof ObservationViewHolder) {
            ObservationViewHolder vh = (ObservationViewHolder) holder;

            if (vh.iconTask != null) {
                vh.iconTask.cancel(false);
            }

            if (vh.userTask != null) {
                vh.userTask.cancel(false);
            }

            if (vh.primaryPropertyTask != null) {
                vh.primaryPropertyTask.cancel(false);
            }

            if (vh.secondaryPropertyTask != null) {
                vh.secondaryPropertyTask.cancel(false);
            }
        }
    }

    private void bindObservationViewHolder(RecyclerView.ViewHolder holder, int position) {
        cursor.moveToPosition(position);
        ObservationViewHolder vh = (ObservationViewHolder) holder;

        try {

            final Observation observation = query.mapRow(new AndroidDatabaseResults(cursor, null, false));
            vh.bind(observation, observationActionListener);

            Drawable markerPlaceholder = DrawableCompat.wrap(ContextCompat.getDrawable(context, R.drawable.ic_place_white_24dp));
            DrawableCompat.setTint(markerPlaceholder, ContextCompat.getColor(context, R.color.icon));
            DrawableCompat.setTintMode(markerPlaceholder, PorterDuff.Mode.SRC_ATOP);

            vh.markerView.setImageDrawable(markerPlaceholder);
            vh.iconTask = new IconTask(vh.markerView);
            vh.iconTask.execute(observation);

            Drawable drawable;
            if (observation.getGeometry().getGeometryType() == GeometryType.POINT) {
                drawable = ResourcesCompat.getDrawable(context.getResources(), R.drawable.ic_place_white_24dp, null);

                vh.shapeView.setImageDrawable(drawable);
                vh.shapeView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                DrawableCompat.setTint(drawable, Color.parseColor("#737373"));
            } else {
                drawable = observation.getGeometry().getGeometryType() == GeometryType.LINESTRING ?
                        ResourcesCompat.getDrawable(context.getResources(), R.drawable.line_string_marker, null) :
                        ResourcesCompat.getDrawable(context.getResources(), R.drawable.polygon_marker, null);

                vh.shapeView.setImageDrawable(drawable);
                vh.shapeView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                int strokeColor = ObservationShapeStyleParser.getStyle(context, observation).getStrokeColor();
                DrawableCompat.setTint(drawable, strokeColor);
            }

            vh.primaryView.setText("");
            vh.primaryPropertyTask = new PropertyTask(PropertyTask.Type.PRIMARY, vh.primaryView);
            vh.primaryPropertyTask.execute(observation);

            vh.secondaryView.setText("");
            vh.secondaryPropertyTask = new PropertyTask(PropertyTask.Type.SECONDARY, vh.secondaryView);
            vh.secondaryPropertyTask.execute(observation);

            Date timestamp = observation.getTimestamp();
            String pattern = DateUtils.isToday(timestamp.getTime()) ? SHORT_TIME_PATTERN : SHORT_DATE_PATTERN;
            DateFormat dateFormat = new SimpleDateFormat(pattern, Locale.getDefault());
            vh.timeView.setText(dateFormat.format(timestamp));

            vh.userView.setText("");
            vh.userTask = new UserTask(vh.userView);
            vh.userTask.execute(observation);

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

    class IconTask extends AsyncTask<Observation, Void, Bitmap> {
        private final WeakReference<ImageView> reference;

        public IconTask(ImageView imageView) {
            this.reference = new WeakReference<>(imageView);
        }

        @Override
        protected Bitmap doInBackground(Observation... observations) {
            return ObservationBitmapFactory.bitmap(context, observations[0]);
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (isCancelled()) {
                bitmap = null;
            }

            ImageView imageView = reference.get();
            if (imageView != null && bitmap != null) {
                imageView.setImageBitmap(bitmap);
            }
        }
    }

    class UserTask extends AsyncTask<Observation, Void, User> {
        private final WeakReference<TextView> reference;

        public UserTask(TextView textView) {
            this.reference = new WeakReference<>(textView);
        }

        @Override
        protected User doInBackground(Observation... observations) {
            User user = null;
            try {
                user = UserHelper.getInstance(context).read(observations[0].getUserId());
            } catch (UserException e) {
                Log.e(LOG_NAME, "Could not get user", e);
            }

            return user;
        }

        @Override
        protected void onPostExecute(User user) {
            if (isCancelled()) {
                user = null;
            }

            TextView textView = reference.get();
            if (textView != null) {
                if (user != null) {
                    textView.setText(user.getDisplayName());
                } else {
                    textView.setText("Unkown User");
                }
            }
        }
    }

    private static class PropertyTask extends AsyncTask<Observation, Void, ObservationProperty> {
        enum Type {
            PRIMARY,
            SECONDARY
        }

        private Type type;
        private final WeakReference<TextView> reference;

        public PropertyTask(Type type, TextView textView) {
            this.type = type;
            this.reference = new WeakReference<>(textView);
        }

        @Override
        protected ObservationProperty doInBackground(Observation... observations) {
            return type == Type.PRIMARY ? observations[0].getPrimaryField() : observations[0].getSecondaryField();
        }

        @Override
        protected void onPostExecute(ObservationProperty property) {
            if (isCancelled()) {
                property = null;
            }

            TextView textView = reference.get();
            if (textView != null) {
                if (property == null || property.isEmpty()) {
                    textView.setVisibility(View.GONE);
                } else {
                    textView.setText(property.getValue().toString());
                    textView.setVisibility(View.VISIBLE);
                }

                textView.requestLayout();
            }
        }
    }
}
