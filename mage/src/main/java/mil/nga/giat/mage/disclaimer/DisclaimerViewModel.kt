package mil.nga.giat.mage.disclaimer

import android.app.Application
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import mil.nga.giat.mage.R
import mil.nga.giat.mage.di.SessionController
import javax.inject.Inject

/**
 * ViewModel for the disclaimer message on app login
 */
data class Disclaimer(val title: String, val text: String)

sealed interface DisclaimerEvent {
   data object NavigateToEvents : DisclaimerEvent
   data object NavigateToLogin : DisclaimerEvent
}

@HiltViewModel
class DisclaimerViewModel @Inject constructor(
   private val application: Application,
   private val preferences: SharedPreferences,
   private val sessionController: SessionController
) : ViewModel() {

   private val _disclaimer = MutableStateFlow<Disclaimer?>(null)
   val disclaimer: StateFlow<Disclaimer?> = _disclaimer

   private val _events = MutableSharedFlow<DisclaimerEvent>()
   val events: SharedFlow<DisclaimerEvent> = _events

   fun checkDisclaimer() {
      val showDisclaimer = preferences.getBoolean(application.getString(R.string.serverDisclaimerShow), false)
      if (showDisclaimer) {
         _disclaimer.value = Disclaimer(
            title = preferences.getString(application.getString(R.string.serverDisclaimerTitle), null).orEmpty(),
            text = preferences.getString(application.getString(R.string.serverDisclaimerText), null).orEmpty()
         )
      } else {
         agree()
      }
   }

   fun agree() {
      preferences.edit { putBoolean(application.getString(R.string.disclaimerAcceptedKey), true) }
      viewModelScope.launch { _events.emit(DisclaimerEvent.NavigateToEvents) }
   }

   fun disagree() {
      sessionController.onLogout(true)
      viewModelScope.launch { _events.emit(DisclaimerEvent.NavigateToLogin) }
   }
}
