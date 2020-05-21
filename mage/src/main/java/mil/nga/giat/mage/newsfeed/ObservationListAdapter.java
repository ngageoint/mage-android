package mil.nga.giat.mage.newsfeed;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.RecyclerView;

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
import mil.nga.giat.mage.sdk.datastore.observation.Observation;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationError;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationFavorite;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationHelper;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationImportant;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationProperty;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.datastore.user.UserHelper;
import mil.nga.giat.mage.sdk.exceptions.ObservationException;
import mil.nga.giat.mage.sdk.exceptions.UserException;

/**
 * Created by wnewman on 10/30/17.
 */

public class ObservationListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface ObservationActionListener {
        void onObservationClick(Observation observation);
        void onObservationImportant(Observation observation);
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
        private TextView primaryView;
        private TextView timeView;
        private TextView secondaryView;
        private TextView userView;
        private View importantView;
        private TextView importantOverline;
        private TextView importantDescription;
        private ImageView importantButton;
        private View syncBadge;
        private View errorBadge;
        private LinearLayout attachmentLayout;
        private ImageView favoriteButton;
        private TextView favoriteCount;
        private View directionsButton;
        private IconTask iconTask;
        private UserTask userTask;
        private PropertyTask primaryPropertyTask;
        private PropertyTask secondaryPropertyTask;

        ObservationViewHolder(View view) {
            super(view);

            markerView = view.findViewById(R.id.observation_marker);
            primaryView = view.findViewById(R.id.primary);
            timeView = view.findViewById(R.id.time);
            secondaryView = view.findViewById(R.id.secondary);
            userView = view.findViewById(R.id.user);
            importantView = view.findViewById(R.id.important);
            importantOverline = view.findViewById(R.id.important_overline);
            importantDescription = view.findViewById(R.id.important_description);
            importantButton = view.findViewById(R.id.important_button);
            syncBadge = view.findViewById(R.id.sync_status);
            errorBadge = view.findViewById(R.id.error_status);
            attachmentLayout = view.findViewById(R.id.image_gallery);
            favoriteButton = view.findViewById(R.id.favorite_button);
            favoriteCount = view.findViewById(R.id.favorite_count);
            directionsButton = view.findViewById(R.id.directions_button);
        }

        public void bind(final Observation observation, final ObservationActionListener listener) {
            itemView.setOnClickListener(v -> listener.onObservationClick(observation));
        }
    }

    private class FooterViewHolder extends RecyclerView.ViewHolder {
        private TextView footerText;

        FooterViewHolder(View view) {
            super(view);

            footerText = view.findViewById(R.id.footer_text);
        }
    }

    public ObservationListAdapter(Context context, AttachmentGallery attachmentGallery, ObservationActionListener observationActionListener) {
        this.context = context;
        this.attachmentGallery = attachmentGallery;
        this.observationActionListener = observationActionListener;
    }

    public void setCursor(Cursor cursor, PreparedQuery<Observation> query) {
        closeCursor();

        this.cursor = cursor;
        this.query = query;
        this.notifyDataSetChanged();
    }

    public void closeCursor() {
        if (cursor != null) {
            cursor.close();
            cursor = null;
        }
    }

    public void setFooterText(String footerText) {
        this.footerText = footerText;
    }

    @Override
    public int getItemViewType(int position) {
        if (cursor != null && position == cursor.getCount()) {
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
        if (cursor == null) return 0;

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
        if (cursor == null) return;

        cursor.moveToPosition(position);
        ObservationViewHolder vh = (ObservationViewHolder) holder;

        try {
            final Observation observation = query.mapRow(new AndroidDatabaseResults(cursor, null, false));
            vh.bind(observation, observationActionListener);

            Drawable markerPlaceholder = DrawableCompat.wrap(ContextCompat.getDrawable(context, R.drawable.ic_place_white_48dp));
            DrawableCompat.setTint(markerPlaceholder, ContextCompat.getColor(context, R.color.icon));
            DrawableCompat.setTintMode(markerPlaceholder, PorterDuff.Mode.SRC_IN);

            vh.markerView.setImageDrawable(markerPlaceholder);
            vh.iconTask = new IconTask(vh.markerView);
            vh.iconTask.execute(observation);

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

            vh.importantButton.setOnClickListener(v -> observationActionListener.onObservationImportant(observation));
            setImportantView(observation.getImportant(), vh);

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

            vh.favoriteButton.setOnClickListener(v -> toggleFavorite(observation, vh));
            setFavoriteImage(observation.getFavorites(), vh, isFavorite(observation));

            vh.directionsButton.setOnClickListener(v -> getDirections(observation));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void bindFooterViewHolder(RecyclerView.ViewHolder holder) {
        FooterViewHolder vh = (FooterViewHolder) holder;

        vh.footerText.setText(footerText);
    }

    private void setImportantView(ObservationImportant important, ObservationViewHolder vh) {
        boolean isImportant = important != null && important.isImportant();
        vh.importantView.setVisibility(isImportant ? View.VISIBLE : View.GONE);

        if (isImportant) {
            try {
                User user = UserHelper.getInstance(context).read(important.getUserId());
                vh.importantOverline.setText(String.format("FLAGGED BY %s", user.getDisplayName().toUpperCase()));
            } catch (UserException e) {
                e.printStackTrace();
            }

            vh.importantDescription.setText(important.getDescription());
        }

        if (isImportant) {
            vh.importantButton.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_flag_white_24dp));
            vh.importantButton.setColorFilter(ContextCompat.getColor(context, R.color.observation_flag_active));
        } else {
            vh.importantButton.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_flag_outlined_white_24dp));
            vh.importantButton.setColorFilter(ContextCompat.getColor(context, R.color.observation_flag_inactive));
        }
    }

    private void setFavoriteImage(Collection<ObservationFavorite> favorites, ObservationViewHolder vh, boolean isFavorite) {
        if (isFavorite) {
            vh.favoriteButton.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_favorite_white_24dp));
            vh.favoriteButton.setColorFilter(ContextCompat.getColor(context, R.color.observation_favorite_active));
        } else {
            vh.favoriteButton.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_favorite_border_white_24dp));
            vh.favoriteButton.setColorFilter(ContextCompat.getColor(context, R.color.observation_favorite_inactive));
        }

        vh.favoriteCount.setVisibility(favorites.size() > 0 ? View.VISIBLE : View.GONE);
        vh.favoriteCount.setText(String.format(Locale.getDefault(), "%d", favorites.size()));
    }

    private void toggleFavorite(Observation observation, ObservationViewHolder vh) {
        ObservationHelper observationHelper = ObservationHelper.getInstance(context);
        boolean isFavorite = isFavorite(observation);
        try {
            if (isFavorite) {
                observationHelper.unfavoriteObservation(observation, currentUser);
            } else {
                observationHelper.favoriteObservation(observation, currentUser);
            }

            setFavoriteImage(observation.getFavorites(), vh, isFavorite);
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
            return type == Type.PRIMARY ? observations[0].getPrimaryFeedField() : observations[0].getSecondaryFeedField();
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
