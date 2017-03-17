package mil.nga.giat.mage.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

import mil.nga.giat.mage.R;

/**
 * Created by barela on 3/17/17.
 */

public class DateFormatFactory {
    public static final DateFormat format(String format, Locale locale, Context context) {
        return DateFormatFactory.format(format, locale, DateFormatFactory.getTimeZone(context));
    }

    public static final DateFormat format(String format, Locale locale, TimeZone timeZone) {
        DateFormat dateFormat = new SimpleDateFormat(format, locale);
        dateFormat.setTimeZone(timeZone);
        return dateFormat;
    }

    public static final TimeZone getTimeZone(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        int timeZone = preferences.getInt(context.getResources().getString(R.string.timeZoneKey), 0);
        // if the preference says local do this
        if (timeZone == 0) {
            return TimeZone.getDefault();
        }
        return TimeZone.getTimeZone("Zulu");
    }
}
