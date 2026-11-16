package mil.nga.giat.mage.disclaimer

import android.app.Application
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit4.MockKRule
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mil.nga.giat.mage.R
import mil.nga.giat.mage.di.SessionController
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class DisclaimerViewModelTest {

   @get:Rule
   val mockkRule = MockKRule(this)

   private val testScheduler = TestCoroutineScheduler()
   private val testDispatcher = StandardTestDispatcher(testScheduler)
   private val testScope = TestScope(testDispatcher + Job())

   @MockK private lateinit var mockApplication: Application
   @MockK private lateinit var mockPreferences: SharedPreferences
   @MockK private lateinit var mockEditor: SharedPreferences.Editor
   @MockK private lateinit var mockSessionController: SessionController

   private lateinit var viewModel: DisclaimerViewModel

   private val title = "Server Disclaimer"
   private val text = "You must agree to the terms before continuing"

   @Before
   fun setUp() {
      Dispatchers.setMain(testDispatcher)

      every { mockApplication.getString(R.string.serverDisclaimerShow) } returns "serverDisclaimerShow"
      every { mockApplication.getString(R.string.serverDisclaimerTitle) } returns "serverDisclaimerTitle"
      every { mockApplication.getString(R.string.serverDisclaimerText) } returns "serverDisclaimerText"
      every { mockApplication.getString(R.string.disclaimerAcceptedKey) } returns "disclaimerAcceptedKey"

      every { mockPreferences.edit() } returns mockEditor
      every { mockEditor.putBoolean(any(), any()) } returns mockEditor
      every { mockEditor.apply() } returns Unit

      viewModel = DisclaimerViewModel(mockApplication, mockPreferences, mockSessionController)
   }

   @After
   fun tearDown() {
      Dispatchers.resetMain()
      testScope.coroutineContext[Job]?.cancel()
   }

   @Test
   fun `checkDisclaimer sets disclaimer state when server disclaimer should show`() {
      every { mockPreferences.getBoolean("serverDisclaimerShow", false) } returns true
      every { mockPreferences.getString("serverDisclaimerTitle", null) } returns title
      every { mockPreferences.getString("serverDisclaimerText", null) } returns text

      viewModel.checkDisclaimer()

      assertEquals(Disclaimer(title, text), viewModel.disclaimer.value)
   }

   @Test
   fun `checkDisclaimer agrees automatically when server disclaimer should not show`() = testScope.runTest {
      every { mockPreferences.getBoolean("serverDisclaimerShow", false) } returns false

      val collectedEvents = mutableListOf<DisclaimerEvent>()
      val collectJob = launch { viewModel.events.collect { collectedEvents.add(it) } }

      viewModel.checkDisclaimer()
      advanceUntilIdle()

      assertNull(viewModel.disclaimer.value)
      verify { mockEditor.putBoolean("disclaimerAcceptedKey", true) }
      assertEquals(listOf(DisclaimerEvent.NavigateToEvents), collectedEvents)

      collectJob.cancel()
   }

   @Test
   fun `agree persists accepted pref and emits NavigateToEvents`() = testScope.runTest {
      val collectedEvents = mutableListOf<DisclaimerEvent>()
      val collectJob = launch { viewModel.events.collect { collectedEvents.add(it) } }

      viewModel.agree()
      advanceUntilIdle()

      verify { mockEditor.putBoolean("disclaimerAcceptedKey", true) }
      assertEquals(listOf(DisclaimerEvent.NavigateToEvents), collectedEvents)

      collectJob.cancel()
   }

   @Test
   fun `disagree logs out and emits NavigateToLogin`() = testScope.runTest {
      every { mockSessionController.onLogout(true) } returns Unit

      val collectedEvents = mutableListOf<DisclaimerEvent>()
      val collectJob = launch { viewModel.events.collect { collectedEvents.add(it) } }

      viewModel.disagree()
      advanceUntilIdle()

      verify { mockSessionController.onLogout(true) }
      assertEquals(listOf(DisclaimerEvent.NavigateToLogin), collectedEvents)

      collectJob.cancel()
   }
}
