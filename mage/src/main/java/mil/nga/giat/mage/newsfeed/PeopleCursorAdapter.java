package mil.nga.giat.mage.newsfeed;

import android.content.Context;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.j256.ormlite.android.AndroidDatabaseResults;
import com.j256.ormlite.stmt.PreparedQuery;

import org.ocpsoft.prettytime.PrettyTime;

import java.sql.SQLException;
import java.text.DateFormat;
import java.util.Locale;
import java.util.TimeZone;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.sdk.datastore.location.Location;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.utils.DateFormatFactory;
import mil.nga.giat.mage.sdk.utils.MediaUtility;

public class PeopleCursorAdapter extends CursorAdapter {
	private static final String LOG_NAME = PeopleCursorAdapter.class.getName();
	
	private LayoutInflater inflater = null;
	private PreparedQuery<Location> query;
    private final DateFormat dateFormat = DateFormatFactory.format("yyyy-MM-dd HH:mm zz", Locale.getDefault(), TimeZone.getTimeZone("Zulu"));

	public PeopleCursorAdapter(Context context, Cursor c, PreparedQuery<Location> query) {
		super(context, c, false);
		this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		this.query = query;
	}

	@Override
	public void bindView(View v, final Context context, Cursor cursor) {
		try {
			Location location = query.mapRow(new AndroidDatabaseResults(cursor, null));
			User user = location.getUser();

			ImageView iconView = (ImageView) v.findViewById(R.id.iconImageView);
			if (location.getUser().getLocalIconPath() != null) {
				iconView.setImageBitmap(MediaUtility.resizeAndRoundCorners(BitmapFactory.decodeFile(location.getUser().getLocalIconPath()), 128));
				iconView.setVisibility(View.VISIBLE);
			} else {
				iconView.setVisibility(View.GONE);
			}

			TextView location_name = (TextView) v.findViewById(R.id.location_name);
			location_name.setText(user.getFirstname() + " " + user.getLastname());

			TextView location_email = (TextView) v.findViewById(R.id.location_email);
			String email = user.getEmail();
			if (email != null && !email.trim().isEmpty()) {
				location_email.setVisibility(View.VISIBLE);
				location_email.setText(email);
			} else {
				location_email.setVisibility(View.GONE);
			}
			
			// set date
			TextView location_date = (TextView) v.findViewById(R.id.location_date);

			String timeText = new PrettyTime().format(location.getTimestamp());

			location_date.setText(timeText);
		} catch (SQLException sqle) {
			Log.e(LOG_NAME, "Could not set location view informaiton.", sqle);
		}
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parentView) {
		return inflater.inflate(R.layout.people_list_item, parentView, false);
	}
}
