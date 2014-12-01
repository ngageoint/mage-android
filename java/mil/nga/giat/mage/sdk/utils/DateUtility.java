package mil.nga.giat.mage.sdk.utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

public class DateUtility {

	static DateFormat ISO8601 = null;

	public static final DateFormat getISO8601() {
		if (ISO8601 == null) {
			ISO8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
			ISO8601.setTimeZone(TimeZone.getTimeZone("Zulu"));
		}
		return ISO8601;
	}
}
