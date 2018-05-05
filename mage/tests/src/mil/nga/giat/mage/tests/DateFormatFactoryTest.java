package mil.nga.giat.mage.tests;

import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;

import junit.framework.TestCase;

import java.text.ParseException;
import java.util.Date;

import mil.nga.giat.mage.sdk.utils.ISO8601DateFormatFactory;

/**
 *
 * Unit test that tests the {@link mil.nga.giat.mage.sdk.utils.ISO8601DateFormatFactory}
 *
 * @author wiedemanns
 */
public class DateFormatFactoryTest extends TestCase {

	private static final String LOG_NAME = DateFormatFactoryTest.class.getName();

	@SmallTest
	public void testISO8601() {
		Date nowBefore = new Date();
		Date nowAfter = null;
		try {
			nowAfter = ISO8601DateFormatFactory.ISO8601().parse(ISO8601DateFormatFactory.ISO8601().format(nowBefore));
		} catch(ParseException pe) {
			Log.e(LOG_NAME, "Could not parse date.");
		}

		assertEquals(nowBefore, nowAfter);
	}
}