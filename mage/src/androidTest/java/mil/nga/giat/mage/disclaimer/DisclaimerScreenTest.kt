package mil.nga.giat.mage.disclaimer

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.preference.PreferenceManager
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import mil.nga.giat.mage.R
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests the disclaimer screen shown to a user when the first log in
 */
@RunWith(AndroidJUnit4::class)
class DisclaimerScreenTest {

   @get:Rule
   val composeTestRule = createEmptyComposeRule()

   private lateinit var scenario: ActivityScenario<DisclaimerActivity>

   private val title = "Server Disclaimer"
   private val text = "You must agree to the terms before continuing"

   @Before
   fun seedDisclaimerPrefsAndLaunch() {
      // seeds prefs before launch, since onResume reads them before @Before would otherwise run
      val context = InstrumentationRegistry.getInstrumentation().targetContext
      PreferenceManager.getDefaultSharedPreferences(context)
         .edit()
         .putBoolean(context.getString(R.string.serverDisclaimerShow), true)
         .putString(context.getString(R.string.serverDisclaimerTitle), title)
         .putString(context.getString(R.string.serverDisclaimerText), text)
         .commit()

      scenario = ActivityScenario.launch(DisclaimerActivity::class.java)
   }

   @After
   fun tearDown() {
      scenario.close()
   }

   @Test
   fun displaysTitleAndBodyText() {
      composeTestRule.onNodeWithText(title).assertExists()
      composeTestRule.onNodeWithText(text).assertExists()
   }

   @Test
   fun displaysAgreeAndDisagreeButtons() {
      val context = InstrumentationRegistry.getInstrumentation().targetContext
      composeTestRule.onNodeWithText(context.getString(R.string.agree)).assertExists()
      composeTestRule.onNodeWithText(context.getString(R.string.disagree)).assertExists()
   }

   @Test
   fun clickingAgree_finishesActivity() {
      val context = InstrumentationRegistry.getInstrumentation().targetContext

      composeTestRule.onNodeWithText(context.getString(R.string.agree)).performClick()
      composeTestRule.waitForIdle()
      waitUntilDestroyed()

      assertEquals(Lifecycle.State.DESTROYED, scenario.state)
   }

   @Test
   fun clickingDisagree_finishesActivity() {
      val context = InstrumentationRegistry.getInstrumentation().targetContext

      composeTestRule.onNodeWithText(context.getString(R.string.disagree)).performClick()
      composeTestRule.waitForIdle()
      waitUntilDestroyed()

      assertEquals(Lifecycle.State.DESTROYED, scenario.state)
   }

   /**
    * agree()/disagree() navigate to real activities (EventsActivity/LoginActivity) before
    * finishing DisclaimerActivity.  How long that takes to fully reach DESTROYED varies by
    * target activity, so poll instead of asserting immediately after waitForIdle().
    */
   private fun waitUntilDestroyed(timeoutMs: Long = 5000, pollIntervalMs: Long = 50) {
      val deadline = System.currentTimeMillis() + timeoutMs
      while (scenario.state != Lifecycle.State.DESTROYED && System.currentTimeMillis() < deadline) {
         Thread.sleep(pollIntervalMs)
      }
   }
}
