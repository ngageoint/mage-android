package mil.nga.giat.mage.map.preference;


import android.content.Context;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.test.espresso.Espresso;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import junit.framework.AssertionFailedError;

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

@RunWith(AndroidJUnit4.class)
@LargeTest
public class OnlineLayersPreferenceActivityTest {

    @Rule
    public ActivityScenarioRule<OnlineLayersPreferenceActivity> activityRule =
            new ActivityScenarioRule(OnlineLayersPreferenceActivity.class);

    private static Layer ourSecureImageryLayer;
    private static Layer ourNonSecureImageryLayer;

    @BeforeClass
    public static void setup() throws Exception{
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
        Espresso.onData(withId(android.R.id.list));
        Espresso.onData(withId(R.id.insecure_layers_list));
    }

}
