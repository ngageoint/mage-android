package mil.nga.giat.mage.disclaimer

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import mil.nga.giat.mage.R
import mil.nga.giat.mage.ui.theme.MageTheme3
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests the disclaimer screen's rendering and button behavior
 */
@RunWith(AndroidJUnit4::class)
class DisclaimerScreenTest {

   @get:Rule
   val composeTestRule = createComposeRule()

   private val title = "Server Disclaimer"
   private val text = "You must agree to the terms before continuing"

   private fun setContent(onAgree: () -> Unit = {}, onDisagree: () -> Unit = {}) {
      composeTestRule.setContent {
         MageTheme3 {
            DisclaimerScreen(
               title = title,
               text = text,
               onAgree = onAgree,
               onDisagree = onDisagree
            )
         }
      }
   }

   @Test
   fun displaysTitleAndBodyText() {
      setContent()

      composeTestRule.onNodeWithText(title).assertExists()
      composeTestRule.onNodeWithText(text).assertExists()
   }

   @Test
   fun displaysAgreeAndDisagreeButtons() {
      setContent()

      val context = InstrumentationRegistry.getInstrumentation().targetContext
      composeTestRule.onNodeWithText(context.getString(R.string.agree)).assertExists()
      composeTestRule.onNodeWithText(context.getString(R.string.disagree)).assertExists()
   }

   @Test
   fun clickingAgree_invokesOnAgree() {
      var agreed = false
      setContent(onAgree = { agreed = true })

      val context = InstrumentationRegistry.getInstrumentation().targetContext
      composeTestRule.onNodeWithText(context.getString(R.string.agree)).performClick()

      assertTrue(agreed)
   }

   @Test
   fun clickingDisagree_invokesOnDisagree() {
      var disagreed = false
      setContent(onDisagree = { disagreed = true })

      val context = InstrumentationRegistry.getInstrumentation().targetContext
      composeTestRule.onNodeWithText(context.getString(R.string.disagree)).performClick()

      assertTrue(disagreed)
   }
}
