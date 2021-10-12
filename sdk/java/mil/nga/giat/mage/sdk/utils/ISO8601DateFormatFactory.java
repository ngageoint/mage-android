package mil.nga.giat.mage.sdk.utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

public class ISO8601DateFormatFactory {

	static String ISO8601_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    public static final DateFormat format(String format, Locale locale) {
        DateFormat dateFormat = new SimpleDateFormat(format, locale);
        return dateFormat;
    }

    public static final DateFormat format(String format, Locale locale, TimeZone timeZone) {
        DateFormat dateFormat = ISO8601DateFormatFactory.format(format, Locale.getDefault());
        dateFormat.setTimeZone(timeZone);
        return dateFormat;
    }

	public static final DateFormat ISO8601() {
        return ISO8601DateFormatFactory.format(ISO8601_FORMAT, Locale.getDefault(), TimeZone.getTimeZone("Zulu"));
	}
}
