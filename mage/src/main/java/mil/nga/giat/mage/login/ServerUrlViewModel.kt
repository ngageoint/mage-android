package mil.nga.giat.mage.login

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mil.nga.giat.mage.R
import mil.nga.giat.mage.dagger.module.ApplicationContext
import mil.nga.giat.mage.data.MageDatabase
import mil.nga.giat.mage.network.Resource
import mil.nga.giat.mage.sdk.preferences.ServerApi
import javax.inject.Inject

class ServerUrlViewModel @Inject constructor(
        @ApplicationContext val context: Context,
        val preferences: SharedPreferences,
        val database: MageDatabase
): ViewModel() {
    private var serverApi = ServerApi(context)

    private val _api = MutableLiveData<Resource<Boolean>>()
    val api: LiveData<Resource<Boolean>> = _api
    fun setUrl(url: String) {
        serverApi.validateServerApi(url) { valid, error ->
            if (valid) {
                GlobalScope.launch {
                    withContext(Dispatchers.IO) {
                        database.destroy(context)
                        preferences.edit().putString(context.getString(R.string.serverURLKey), url).apply()
                        _api.postValue(Resource.success(valid))
                    }
                }
            } else {
                if (error == null) {
                    _api.postValue(Resource.success(false))
                } else {
                    _api.postValue(Resource.error(error.cause?.localizedMessage ?: "Cannot connect to server.", false))
                }
            }
        }
    }
}