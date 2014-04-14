package mil.nga.giat.mage.newsfeed;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.map.marker.LocationBitmapFactory;
import mil.nga.giat.mage.sdk.datastore.location.Location;
import mil.nga.giat.mage.sdk.utils.DateUtility;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts.Data;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.j256.ormlite.android.AndroidDatabaseResults;
import com.j256.ormlite.stmt.PreparedQuery;

public class PeopleCursorAdapter extends CursorAdapter {
	private DateFormat iso8601 =  DateUtility.getISO8601();
	private LayoutInflater inflater = null;
    private PreparedQuery<Location> query;
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm zz", Locale.ENGLISH);

	public PeopleCursorAdapter(Context context, Cursor c, PreparedQuery<Location> query) {
        super(context, c, false);
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.query = query;
    }
	
	@Override
	public void bindView(View v, final Context context, Cursor cursor) {
		try {
            Location l = query.mapRow(new AndroidDatabaseResults(cursor, null));
            
            ImageView iconView = (ImageView) v.findViewById(R.id.iconImageView);
            Bitmap iconMarker = LocationBitmapFactory.bitmap(context, l);
            if (iconMarker != null)
                iconView.setImageBitmap(iconMarker); 
            
            TextView user = (TextView)v.findViewById(R.id.username);
            TextView dateView = (TextView)v.findViewById(R.id.location_date);
            
//            v.findViewById(R.id.add_contact).setOnClickListener(new View.OnClickListener() {
//				
//				@Override
//				public void onClick(View v) {
//					Log.i("test", "add contact");
//					
//					Cursor c = context.getContentResolver().query(android.provider.ContactsContract.Data.CONTENT_URI,
//					          new String[] {Data._ID, Phone.NUMBER, Phone.TYPE, Phone.LABEL},
//					          Data.RAW_CONTACT_ID + "=?" + " AND "
//					                  + Data.MIMETYPE + "='" + Phone.CONTENT_ITEM_TYPE + "'",
//					          new String[] {String.valueOf(001)}, null);
//					Log.i("test", "c.getCount() is : " + c.getCount());
//					if (c.moveToFirst()) {
//						Log.i("test", "cursor is: " + c.getString(1));
//					}
//					
//					
////					ContentValues values = new ContentValues();
////			        values.put(Data.RAW_CONTACT_ID, 001);
////			        values.put(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
////			        values.put(Phone.NUMBER, "1-800-GOOG-411");
////			        values.put(Phone.TYPE, Phone.TYPE_CUSTOM);
////			        values.put(Phone.LABEL, "Nirav");
////			        Uri dataUri = context.getContentResolver().insert(android.provider.ContactsContract.Data.CONTENT_URI, values);
//					
//				}
//			});
            
            user.setText(l.getUser().getFirstname() + " " + l.getUser().getLastname());
            String dateText = l.getPropertiesMap().get("timestamp");
            try {
                Date date = iso8601.parse(l.getPropertiesMap().get("timestamp"));
                dateText = sdf.format(date);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            dateView.setText(dateText); 
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parentView) {
		return inflater.inflate(R.layout.people_list_item, parentView, false);
	}
}
