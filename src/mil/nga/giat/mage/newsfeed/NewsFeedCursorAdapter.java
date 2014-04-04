package mil.nga.giat.mage.newsfeed;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

import com.bumptech.glide.Glide;
import com.j256.ormlite.android.AndroidDatabaseResults;
import com.j256.ormlite.stmt.PreparedQuery;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.form.MageTextView;
import mil.nga.giat.mage.observation.ObservationViewActivity;
import mil.nga.giat.mage.sdk.datastore.observation.Attachment;
import mil.nga.giat.mage.sdk.datastore.observation.Observation;
import mil.nga.giat.mage.sdk.preferences.PreferenceHelper;
import mil.nga.giat.mage.sdk.utils.DateUtility;
import mil.nga.giat.mage.sdk.utils.MediaUtility;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class NewsFeedCursorAdapter extends CursorAdapter {
	
	private LayoutInflater inflater = null;
	private PreparedQuery<Observation> query;
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm zz");
	private FragmentActivity activity;
//	
//	public NewsFeedCursorAdapter(FragmentActivity activity) {
//		this.activity = activity;
//		inflater = (LayoutInflater)activity.getLayoutInflater();
//	}
	
	 public NewsFeedCursorAdapter(Context context, Cursor c, PreparedQuery<Observation> query, FragmentActivity activity) {
		super(context, c, false);
	  	inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	  	this.activity = activity;
	  	this.query = query;
	 }
	 
     private void populatePropertyFields(LinearLayout ll, Map<String, String> propertiesMap) {
 		for (int i = 0; i < ll.getChildCount(); i++) {
 			View v = ll.getChildAt(i);
 			if (v instanceof MageTextView) {
 				MageTextView m = (MageTextView)v;
 				String propertyKey = m.getPropertyKey();
 				String propertyValue = propertiesMap.get(propertyKey);
 				if (propertyValue == null) continue;
 				switch(m.getPropertyType()) {
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
 				populatePropertyFields((LinearLayout)v, propertiesMap);
 			}
 		}
 	}
	@Override
	public void bindView(View v, Context context, Cursor cursor) {
		
		try
        {
			
            final Observation o = query.mapRow(new AndroidDatabaseResults(cursor, null));
            
        
		
		
		LinearLayout ll = (LinearLayout)v.findViewById(R.id.observation_list_container);
		populatePropertyFields(ll, o.getPropertiesMap());
		
		ll.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent observationView = new Intent(activity.getApplicationContext(), ObservationViewActivity.class);
				observationView.putExtra(ObservationViewActivity.OBSERVATION_ID, o.getId());
				activity.startActivityForResult(observationView, 2);
			}
		});
		
		ImageView iv = ((ImageView)v.findViewById(R.id.observation_thumb));
		Collection<Attachment> attachments = o.getAttachments();
		((TextView)v.findViewById(R.id.username)).setText("there are " + attachments.size() + " attachments");
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
					String url = a.getUrl() + "?access_token=" + token;
					Log.i("test", "URL: " + url);
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
        } catch(java.sql.SQLException e) {
            e.printStackTrace();
        }

	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parentView) {
		return inflater.inflate(R.layout.observation_list_item, parentView, false);
	}

}
