package mil.nga.giat.mage.map.preference;


import android.content.Context;
import android.view.View;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.instanceOf;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import junit.framework.AssertionFailedError;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.UUID;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.sdk.datastore.layer.Layer;
import mil.nga.giat.mage.sdk.datastore.layer.LayerHelper;
import mil.nga.giat.mage.sdk.datastore.user.Event;
import mil.nga.giat.mage.sdk.datastore.user.EventHelper;

/**
 * There are some current limitations to espresso that can be worked around in an automated
 * fashion.  As such, that automated way is not implemented, and thus the following steps must
 * be run <b>MANUALLY!!!!</b>
 * <p>
 * Ensure you have ADB installed (mac instruction)
 * <p>
 * <pre>
 *     brew cask install android-platform-tools
 * </pre>
 *
 * <p>
 * <pre>
 *     Launch MAGE using an AVD
 *     log in
 * </pre>
 *
 *<p>
 * Assuming you only have 1 adb device (listed via 'adb devices' command), run the following:
 * <p>
 * <pre>
 *     adb shell settings put global window_animation_scale 0 &
 *     adb shell settings put global transition_animation_scale 0 &
 *     adb shell settings put global animator_duration_scale 0 &
 * </pre>
 *
 * @since 10/31/2019
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class OnlineLayersPreferenceActivityTest {

    @Rule
    public ActivityScenarioRule<OnlineLayersPreferenceActivity> activityRule =
            new ActivityScenarioRule(OnlineLayersPreferenceActivity.class);

    /**
     * Test layer objects
     */
    private static Layer ourSecureImageryLayer;
    private static Layer ourNonSecureImageryLayer;

    @BeforeClass
    public static void setup() throws Exception{

        //TODO must log into mage app independent of this test to bypass login creds for now
        Event currentEvent = EventHelper.getInstance(getApplicationContext()).getCurrentEvent();

        Layer secureLayer = new Layer();
        secureLayer.setRemoteId(UUID.randomUUID().toString());
        secureLayer.setLoaded(true);
        secureLayer.setFormat("XYZ");
        secureLayer.setName("Unit Test Secure Layer");
        secureLayer.setType("Imagery");
        secureLayer.setUrl("https://www.google.com");
        secureLayer.setEvent(currentEvent);

        ourSecureImageryLayer = LayerHelper.getInstance(getApplicationContext()).create(secureLayer);

        Layer nonSecureLayer = new Layer();
        nonSecureLayer.setRemoteId(UUID.randomUUID().toString());
        nonSecureLayer.setLoaded(true);
        nonSecureLayer.setFormat("XYZ");
        nonSecureLayer.setName("Unit Test Nonsecure Layer");
        nonSecureLayer.setType("Imagery");
        nonSecureLayer.setUrl("http://www.google.com");
        nonSecureLayer.setEvent(currentEvent);

        ourNonSecureImageryLayer = LayerHelper.getInstance(getApplicationContext()).create(nonSecureLayer);
    }

    @AfterClass
    public static void teardown() throws Exception{
        LayerHelper.getInstance(getApplicationContext()).delete(ourSecureImageryLayer.getId());
        LayerHelper.getInstance(getApplicationContext()).delete(ourNonSecureImageryLayer.getId());
    }

    @Test
    public void testRefresh() throws Exception {
        try {
            onView(withId(R.id.online_layers_no_content_progressBar)).check(matches(isDisplayed()));
            Assert.fail("Progress bar should not be displayed");
        } catch (AssertionFailedError e) {

        }

        onView(withId(R.id.online_layers_refresh)).check(matches(isDisplayed()));
        onView(withId(R.id.online_layers_refresh)).check(matches(isEnabled()));

        Context context = getApplicationContext();

        Assert.assertNotNull(LayerHelper.getInstance(context).read(ourSecureImageryLayer.getId()));
        Assert.assertNotNull(LayerHelper.getInstance(context).read(ourNonSecureImageryLayer.getId()));

        onView(withId(R.id.online_layers_refresh)).perform(click());

        //TODO somehow check to verify that the layers are in the appropriate lists

        //Espresso.onData(equalTo(ourSecureImageryLayer)).onChildView(withId(android.R.id.list)).check(matches(isDisplayed()));
        //Espresso.onData(equalTo(ourNonSecureImageryLayer)).onChildView(withId(R.id.insecure_layers_list)).check(matches(isDisplayed()));

        //Verify a dialog is displayed about a non HTTPS layer
        onData(instanceOf(Layer.class)).inAdapterView(withTag("InsecureView")).atPosition(0).perform(click());
        onView(withText("Non HTTPS Layer")).inRoot(isDialog()).check(matches(isDisplayed()));
        onView(withId(android.R.id.button1)).perform(click());
    }

    /**
     * This helps "find" the lists in the activity, since there is more than 1.
     *
     * @param tag
     * @return
     */
    private static Matcher<View> withTag(final Object tag) {
        return new TypeSafeMatcher<View>() {

            @Override
            public void describeTo(final Description description) {
                description.appendText("has tag equals to: " + tag);
            }

            @Override
            protected boolean matchesSafely(final View view) {
                Object viewTag = view.getTag();
                if (viewTag == null) {
                    return tag == null;
                }

                return viewTag.equals(tag);
            }
        };
    }

}
