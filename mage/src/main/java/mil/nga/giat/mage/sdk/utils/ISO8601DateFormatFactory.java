package mil.nga.giat.mage.sdk.utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

public class ISO8601DateFormatFactory {

	static String ISO8601_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

	public static DateFormat ISO8601() {
      DateFormat dateFormat = new SimpleDateFormat(ISO8601_FORMAT, Locale.getDefault());
      dateFormat.setTimeZone(TimeZone.getTimeZone("Zulu"));
      return dateFormat;
	}
}
