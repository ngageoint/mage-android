package mil.nga.giat.mage.login

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mil.nga.giat.mage.R
import mil.nga.giat.mage.database.MageDatabase
import mil.nga.giat.mage.data.repository.api.ApiRepository
import mil.nga.giat.mage.data.repository.api.ApiResponse
import mil.nga.giat.mage.database.dao.MageSqliteOpenHelper
import mil.nga.giat.mage.network.Resource
import javax.inject.Inject

@HiltViewModel
class ServerUrlViewModel @Inject constructor(
   private val application: Application,
   private val preferences: SharedPreferences,
   private val daoStore: MageSqliteOpenHelper,
   private val database: MageDatabase,
   private val apiRepository: ApiRepository
): ViewModel() {

    private val _api = MutableLiveData<Resource<Boolean>>()
    val api: LiveData<Resource<Boolean>> = _api
    fun setUrl(url: String) {
        viewModelScope.launch(Dispatchers.IO) {
            when (val response = apiRepository.getApi(url)) {
                is ApiResponse.Valid -> {
                    daoStore.resetDatabase()
                    database.destroy()
                    preferences.edit().putString(application.getString(R.string.serverURLKey), url).apply()
                    _api.postValue(Resource.success(true))
                }
                is ApiResponse.Invalid -> {
                    _api.postValue(Resource.success(false))
                }
                is ApiResponse.Error -> {
                    _api.postValue(Resource.error(response.message, false))
                }
            }
        }
    }
}