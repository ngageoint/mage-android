package mil.nga.giat.mage.tests;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.SmallTest;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.login.LoginActivity;

/**
 * Tests parts of {@link mil.nga.giat.mage.login.LoginActivity}
 *
 * @author wiedemanns
 */
public class LoginActivityTest extends ActivityInstrumentationTestCase2<LoginActivity> {

	private static final String LOG_NAME = LoginActivityTest.class.getName();

	LoginActivity activity;

	public LoginActivityTest() {
		super(LoginActivity.class);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		activity = getActivity();
	}

	/**
	 * Server url should be set
	 *
	 * @see mil.nga.giat.mage.login.LoginActivity#getServerEditText()
	 */
	@SmallTest
	public void testServerEditText() {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext());
		assertEquals(activity.getServerEditText().getText().toString(), sharedPreferences.getString(activity.getString(R.string.serverURLKey), activity.getString(R.string.serverURLDefaultValue)));
	}
}
