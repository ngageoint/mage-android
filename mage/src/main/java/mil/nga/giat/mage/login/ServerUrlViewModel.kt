package mil.nga.giat.mage.login

import android.app.Application
import android.content.SharedPreferences
import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mil.nga.giat.mage.R
import mil.nga.giat.mage.data.datasource.observation.AttachmentLocalDataSource
import mil.nga.giat.mage.data.datasource.observation.ObservationLocalDataSource
import mil.nga.giat.mage.database.MageDatabase
import mil.nga.giat.mage.data.repository.api.ApiRepository
import mil.nga.giat.mage.data.repository.api.ApiResponse
import mil.nga.giat.mage.database.dao.MageSqliteOpenHelper
import mil.nga.giat.mage.ui.setup.AdminContact
import javax.inject.Inject

sealed class UrlState {
   data object Valid: UrlState()
   data object Invalid: UrlState()
   data object InProgress: UrlState()
   data class Error(val statusCode: Int?, val message: String?): UrlState()
   data class Incompatible(val version: String, val contact: AdminContact): UrlState()
}

@HiltViewModel
class ServerUrlViewModel @Inject constructor(
   private val application: Application,
   private val preferences: SharedPreferences,
   private val adminContact: AdminContact,
   private val daoStore: MageSqliteOpenHelper,
   private val database: MageDatabase,
   private val apiRepository: ApiRepository,
   private val observationLocalDataSource: ObservationLocalDataSource,
   private val attachmentLocalDataSource: AttachmentLocalDataSource
): ViewModel() {
   val url = preferences.getString(application.getString(R.string.serverURLKey), application.getString(R.string.serverURLDefaultValue)) ?: ""
   val version = preferences.getString(application.getString(R.string.buildVersionKey), null)

   private val _unsavedData = MutableLiveData<Boolean>()
   val unsavedData: LiveData<Boolean> = _unsavedData

   init {
      viewModelScope.launch(Dispatchers.IO) {
         val unsavedObservations = observationLocalDataSource.dirty
         val unsavedAttachments = attachmentLocalDataSource.dirtyAttachments
         if (unsavedObservations.isNotEmpty() || unsavedAttachments.isNotEmpty()) {
            _unsavedData.postValue(true)
         }
      }
   }

   fun confirmUnsavedData() {
      _unsavedData.value = false
   }

   private val _urlState = MutableLiveData<UrlState>()
   val urlState: LiveData<UrlState> = _urlState

   fun checkUrl(url: String) {
      if (Patterns.WEB_URL.matcher(url).matches()) {
         _urlState.value = UrlState.InProgress

         var processedUrl:String = url
         if (!processedUrl.contains("://")) {
            processedUrl = "https://" + url
         }
         if (processedUrl.endsWith("/")) {
            processedUrl = processedUrl.dropLast(1)
         }

         viewModelScope.launch(Dispatchers.IO) {
            when (val response = apiRepository.getApi(processedUrl)) {
               is ApiResponse.Success -> {
                  daoStore.resetDatabase()
                  database.destroy()
                  preferences
                     .edit()
                     .putString(application.getString(R.string.serverURLKey), processedUrl)
                     .apply()

                  _urlState.postValue(UrlState.Valid)
               }
               is ApiResponse.Incompatible -> {
                  val state = UrlState.Incompatible(
                     version = response.version,
                     contact = adminContact
                  )
                  _urlState.postValue(state)
               }
               is ApiResponse.Error -> {
                  _urlState.postValue(UrlState.Error(response.statusCode, response.message))
               }
            }
         }
      } else {
         _urlState.postValue(UrlState.Invalid)
      }
    }
}