package mil.nga.giat.mage.disclaimer

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.preference.PreferenceManager
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import io.mockk.every
import io.mockk.mockk
import mil.nga.giat.mage.R
import mil.nga.giat.mage.di.SessionController
import mil.nga.giat.mage.di.SessionControllerModule
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests the disclaimer screen shown to a user when they first log in.
 *
 * Runs under HiltTestApplication (see HiltTestRunner), so MageApplication's own onCreate() -
 * including its force-login ActivityLifecycleCallbacks - never runs, and DisclaimerActivity gets
 * a fake SessionController instead of the real MageApplication, so clicking Disagree doesn't
 * touch real session/token/local-database state.
 */
@HiltAndroidTest
@UninstallModules(SessionControllerModule::class)
@RunWith(AndroidJUnit4::class)
class DisclaimerScreenTest {

   @get:Rule(order = 0)
   val hiltRule = HiltAndroidRule(this)

   @get:Rule(order = 1)
   val composeTestRule = createEmptyComposeRule()

   @BindValue
   @JvmField
   val sessionController: SessionController = mockk(relaxed = true)

   private lateinit var scenario: ActivityScenario<DisclaimerActivity>

   private val title = "Server Disclaimer"
   private val text = "You must agree to the terms before continuing"

   @Before
   fun seedDisclaimerPrefsAndLaunch() {
      hiltRule.inject()

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
      every { sessionController.onLogout(any()) } returns Unit

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
