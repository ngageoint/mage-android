package mil.nga.giat.mage.map.preference;

import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;

import javax.inject.Inject;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.data.datasource.event.EventLocalDataSource;
import mil.nga.giat.mage.data.datasource.permission.RoleLocalDataSource;
import mil.nga.giat.mage.data.datasource.user.UserLocalDataSource;
import mil.nga.giat.mage.database.model.event.Event;
import mil.nga.giat.mage.database.model.permission.Role;
import mil.nga.giat.mage.database.model.user.User;
import mil.nga.giat.mage.sdk.datastore.user.Permissions;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;

/**
 * @see OnlineLayersPreferenceActivityTest
 * @since 11/22/19
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
@LargeTest
public class OfflineLayersPreferenceActivityTest {

    private static final String SEED_REMOTE_ID = "androidTest-offlineLayers-seed";

    private final HiltAndroidRule hiltRule = new HiltAndroidRule(this);
    private final ActivityScenarioRule<TileOverlayPreferenceActivity> activityRule =
            new ActivityScenarioRule<>(TileOverlayPreferenceActivity.class);

    // Runs hiltRule before activityRule, so the Hilt component exists before the activity under test is launched.
    @Rule
    public RuleChain rules = RuleChain.outerRule(hiltRule).around(activityRule);

    @Inject
    RoleLocalDataSource roleLocalDataSource;

    @Inject
    UserLocalDataSource userLocalDataSource;

    @Inject
    EventLocalDataSource eventLocalDataSource;

    @Before
    public void seedCurrentUserAndEvent() throws Exception {
        hiltRule.inject();

        Role role = roleLocalDataSource.createOrUpdate(
                new Role(SEED_REMOTE_ID, "androidTest-role", "Seeded by OfflineLayersPreferenceActivityTest", new Permissions()));
        User user = userLocalDataSource.createOrUpdate(
                new User(SEED_REMOTE_ID, "androidTest-user", "Android Test User", null, null, null, null, null, role));
        Event event = eventLocalDataSource.createOrUpdate(
                new Event(SEED_REMOTE_ID, "androidTest-event", "Seeded by OfflineLayersPreferenceActivityTest", null));

        userLocalDataSource.setCurrentUser(user);
        userLocalDataSource.setCurrentEvent(user, event);
    }

    @Test
    public void testRefresh() throws Exception {
        onView(withId(R.id.tile_overlay_refresh)).check(matches(isDisplayed()));
        Thread.sleep(5000);
        onView(withId(R.id.tile_overlay_refresh)).check(matches(isEnabled()));

        onView(withId(R.id.tile_overlay_refresh)).perform(click());
        onView(withId(R.id.tile_overlay_refresh)).check(matches(isEnabled()));

        onView(withId(R.id.downloadable_layers_content)).check(matches(isDisplayed()));
        onView(withId(R.id.downloadable_layers_no_content)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));
    }
}
