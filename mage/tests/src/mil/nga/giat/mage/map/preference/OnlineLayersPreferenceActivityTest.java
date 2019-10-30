package mil.nga.giat.mage.map.preference;


import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import mil.nga.giat.mage.R;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class OnlineLayersPreferenceActivityTest {

    @Rule
    public ActivityScenarioRule<OnlineLayersPreferenceActivity> activityRule =
            new ActivityScenarioRule(OnlineLayersPreferenceActivity.class);

    @Before
    public void setup() {
        //TODO load layers?
    }

    @After
    public void teardown() {
    }

    @Test
    public void testRefresh(){
        onView(withId(R.id.online_layers_refresh)).check(matches(isDisplayed()));
        onView(withId(R.id.online_layers_refresh)).check(matches(isEnabled()));
    }

}
