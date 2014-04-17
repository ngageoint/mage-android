package mil.nga.giat.mage.newsfeed;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.form.MageTextView;
import mil.nga.giat.mage.map.marker.ObservationBitmapFactory;
import mil.nga.giat.mage.sdk.datastore.observation.Attachment;
import mil.nga.giat.mage.sdk.datastore.observation.Observation;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationProperty;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.datastore.user.UserHelper;
import mil.nga.giat.mage.sdk.preferences.PreferenceHelper;
import mil.nga.giat.mage.sdk.utils.DateUtility;
import mil.nga.giat.mage.sdk.utils.MediaUtility;
import android.app.Activity;
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

import com.bumptech.glide.Glide;
import com.j256.ormlite.android.AndroidDatabaseResults;
import com.j256.ormlite.stmt.PreparedQuery;

public class NewsFeedCursorAdapter extends CursorAdapter {

    private LayoutInflater inflater = null;
    private PreparedQuery<Observation> query;
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm zz", Locale.ENGLISH);
    private Activity activity;
    private Observation o;

    public NewsFeedCursorAdapter(Context context, Cursor c, PreparedQuery<Observation> query, Activity activity) {
        super(context, c, false);
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.activity = activity;
        this.query = query;
    }
    
    public boolean hasStableIds() {
    	return true;
    }

    private void populatePropertyFields(LinearLayout ll, Map<String, ObservationProperty> propertiesMap) {
        for (int i = 0; i < ll.getChildCount(); i++) {
            View v = ll.getChildAt(i);
            if (v instanceof MageTextView) {
                MageTextView m = (MageTextView) v;
                String propertyKey = m.getPropertyKey();
                String propertyValue = null;
                if (propertiesMap.containsKey(propertyKey)) {
                	propertyValue = propertiesMap.get(propertyKey).getValue();
                }
                if (propertyValue == null)
                    continue;
                switch (m.getPropertyType()) {
                case STRING:
                case MULTILINE:
                    m.setText(propertyValue);
                    break;
                case USER:

                    break;
                case DATE:
                    try {
                        m.setText(sdf.format(new Date(Long.parseLong(propertyValue))));
                    } catch (NumberFormatException nfe) {
                        try {
                            m.setText(sdf.format(DateUtility.getISO8601().parse(propertyValue)));
                        } catch (java.text.ParseException pe) {
                            pe.printStackTrace();
                        }
                    }
                    break;
                case LOCATION:

                    break;
                case MULTICHOICE:

                    break;
                }
            } else if (v instanceof LinearLayout) {
                populatePropertyFields((LinearLayout) v, propertiesMap);
            }
        }
    }

    @Override
    public void bindView(View v, Context context, Cursor cursor) {
        try {
            o = query.mapRow(new AndroidDatabaseResults(cursor, null));

            LinearLayout ll = (LinearLayout) v.findViewById(R.id.observation_list_container);
            populatePropertyFields(ll, o.getPropertiesMap());

            ImageView markerView = (ImageView) v.findViewById(R.id.observation_marker);
            Bitmap marker = ObservationBitmapFactory.bitmap(activity, o);
            if (marker != null)
                markerView.setImageBitmap(marker);

            ImageView iv = ((ImageView) v.findViewById(R.id.observation_thumb));
            Collection<Attachment> attachments = o.getAttachments();
            if (o.getPropertiesMap().containsKey("userId")) {
            	String userId = o.getPropertiesMap().get("userId").getValue();
            	try {
	            	User u = UserHelper.getInstance(context).read(userId);
	            	((TextView) v.findViewById(R.id.username)).setText(u.getFirstname() + " " + u.getLastname());
            	} catch (Exception e) {
            		((TextView) v.findViewById(R.id.username)).setText("Unknown");
            	}
            } else {
            	((TextView) v.findViewById(R.id.username)).setText("Unknown");
            }
            ((TextView) v.findViewById(R.id.attachment_text)).setText(attachments.size() != 0 ? "1 of " + attachments.size() : "");
            if (attachments.size() != 0) {
                iv.setVisibility(View.VISIBLE);
                Attachment a = attachments.iterator().next();

                String token = PreferenceHelper.getInstance(activity.getApplicationContext()).getValue(R.string.tokenKey);

                final String absPath = a.getLocalPath();
                final String remoteId = a.getRemoteId();

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

                if (absPath != null) {
                    if (contentType.startsWith("image")) {
                        Glide.load(new File(absPath)).placeholder(android.R.drawable.progress_indeterminate_horizontal).centerCrop().into(iv);
                    } else if (contentType.startsWith("video")) {
                        Glide.load(R.drawable.ic_video_2x).into(iv);
                    } else if (contentType.startsWith("audio")) {
                        Glide.load(R.drawable.ic_microphone).into(iv);
                    }
                } else if (remoteId != null) {
                    if (contentType.startsWith("image")) {
                        String url = a.getUrl();
                        Log.i("News Feed", "Loading Image URL: " + url);
                        Glide.load(url).placeholder(android.R.drawable.progress_indeterminate_horizontal).centerCrop().into(iv);
                    } else if (contentType.startsWith("video")) {
                        Glide.load(R.drawable.ic_video_2x).into(iv);
                    } else if (contentType.startsWith("audio")) {
                        Glide.load(R.drawable.ic_microphone).into(iv);
                    }
                }
            } else {
                iv.setVisibility(View.GONE);
                iv.setImageDrawable(null);
            }
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parentView) {
        return inflater.inflate(R.layout.observation_list_item, parentView, false);
    }

}