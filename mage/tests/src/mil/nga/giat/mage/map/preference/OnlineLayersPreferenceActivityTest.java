package mil.nga.giat.mage.map.preference;


import android.content.Context;
import android.view.View;
import android.webkit.URLUtil;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.lifecycle.Lifecycle;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.database.model.layer.Layer;
import mil.nga.giat.mage.data.datasource.layer.LayerLocalDataSource;
import mil.nga.giat.mage.database.model.event.Event;
import mil.nga.giat.mage.data.datasource.event.EventLocalDataSource;

/**
 * There are some current limitations to espresso that can be worked around in an automated
 * fashion.  As such, that automated way is not implemented, and thus the following steps must
 * be run <b>MANUALLY!!!!</b>
 * <br><br>
 * Ensure you have ADB installed (mac instruction):
 * <p>
 * <pre>
 *     brew cask install android-platform-tools
 * </pre>
 *
 * <br>
 * Application Interaction:
 * <pre>
 *     Launch MAGE using an AVD
 *     Log in
 *     Select an event
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
// TODO need to setup hilt test injection to mock data sources
@RunWith(AndroidJUnit4.class)
@LargeTest
public class OnlineLayersPreferenceActivityTest {

    @Rule
    public ActivityScenarioRule<OnlineLayersPreferenceActivity> activityRule =
            new ActivityScenarioRule(OnlineLayersPreferenceActivity.class);

    private static final String TYPE = "Imagery";

    /**
     * Test layer objects
     */
    private static Layer ourSecureImageryLayer;
    private static Layer ourNonSecureImageryLayer;

//    @BeforeClass
//    public static void setup() throws Exception{
//
//        //TODO must log into mage app independent of this test to bypass login creds for now
//        Event currentEvent = EventLocalDataSource.getInstance(getApplicationContext()).getCurrentEvent();
//
//        LayerLocalDataSource.getInstance(getApplicationContext()).deleteAll(TYPE);
//
//        Layer secureLayer = new Layer();
//        secureLayer.setRemoteId(UUID.randomUUID().toString());
//        secureLayer.setLoaded(true);
//        secureLayer.setFormat("XYZ");
//        secureLayer.setName("Unit Test Secure Layer");
//        secureLayer.setType(TYPE);
//        secureLayer.setUrl("https://www.google.com");
//        secureLayer.setEvent(currentEvent);
//
//        ourSecureImageryLayer = LayerLocalDataSource.getInstance(getApplicationContext()).create(secureLayer);
//
//        Layer nonSecureLayer = new Layer();
//        nonSecureLayer.setRemoteId(UUID.randomUUID().toString());
//        nonSecureLayer.setLoaded(true);
//        nonSecureLayer.setFormat("XYZ");
//        nonSecureLayer.setName("Unit Test Nonsecure Layer");
//        nonSecureLayer.setType(TYPE);
//        nonSecureLayer.setUrl("http://www.google.com");
//        nonSecureLayer.setEvent(currentEvent);
//
//        ourNonSecureImageryLayer = LayerLocalDataSource.getInstance(getApplicationContext()).create(nonSecureLayer);
//    }

//    @AfterClass
//    public static void teardown() throws Exception{
//        LayerLocalDataSource.getInstance(getApplicationContext()).delete(ourSecureImageryLayer.getId());
//        LayerLocalDataSource.getInstance(getApplicationContext()).delete(ourNonSecureImageryLayer.getId());
//    }

    @Before
    public void before(){
        activityRule.getScenario().moveToState(Lifecycle.State.RESUMED);
    }

//    @Test
//    public void testRefresh() throws Exception {
//
//        onView(withId(R.id.online_layers_refresh)).check(matches(isDisplayed()));
//        onView(withId(R.id.online_layers_refresh)).check(matches(isEnabled()));
//
//        Context context = getApplicationContext();
//
//        Assert.assertNotNull(LayerLocalDataSource.getInstance(context).read(ourSecureImageryLayer.getId()));
//        Assert.assertNotNull(LayerLocalDataSource.getInstance(context).read(ourNonSecureImageryLayer.getId()));
//
//        onView(withId(R.id.online_layers_refresh)).perform(click());
//
//
//        Event currentEvent = EventLocalDataSource.getInstance(getApplicationContext()).getCurrentEvent();
//        List<Layer> imageryLayers = LayerLocalDataSource.getInstance(getApplicationContext()).readByEvent(currentEvent, TYPE);
//
//        List<Layer> secureLayers = new ArrayList<>();
//        List<Layer> insecureLayers = new ArrayList<>();
//
//        for(Layer layer : imageryLayers){
//            if(URLUtil.isHttpUrl(layer.getUrl())){
//                insecureLayers.add(layer);
//            }else{
//                secureLayers.add(layer);
//            }
//        }
//
//        Collections.sort(secureLayers,new LayerNameComparator());
//
//        Collections.sort(insecureLayers, new LayerNameComparator());
//
//        int secureIdx = -1;
//        for(int i = 0 ; i < secureLayers.size(); i++){
//            Layer layer = secureLayers.get(i);
//            if(layer.equals(ourSecureImageryLayer)){
//                secureIdx = i;
//                break;
//            }
//        }
//        Assert.assertNotEquals(-1, secureIdx);
//
//        //Account for 1 section header
//        secureIdx++;
//
//        int insecureIdx = -1;
//        for(int i = 0; i < insecureLayers.size(); i++){
//            Layer layer = insecureLayers.get(i);
//            if(layer.equals(ourNonSecureImageryLayer)){
//                insecureIdx = i;
//                break;
//            }
//        }
//        Assert.assertNotEquals(-1, insecureIdx);
//
//        //Account for 2 section headers
//        insecureIdx += 2;
//
//        //Verify a dialog is displayed about a non HTTPS layer
//        onView(withTag("online")).perform(RecyclerViewActions.actionOnItemAtPosition(insecureIdx,
//                click()));
//        onView(withText("Non HTTPS Layer")).inRoot(isDialog()).check(matches(isDisplayed()));
//        onView(withId(android.R.id.button1)).perform(click());
//    }

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
