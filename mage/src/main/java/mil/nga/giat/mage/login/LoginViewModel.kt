package mil.nga.giat.mage.login

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import mil.nga.giat.mage.dagger.module.ApplicationContext
import mil.nga.giat.mage.sdk.login.AccountStatus
import mil.nga.giat.mage.sdk.login.LdapLoginTask
import mil.nga.giat.mage.sdk.login.LoginTaskFactory
import mil.nga.giat.mage.sdk.preferences.ServerApi
import org.apache.commons.lang3.StringUtils
import javax.inject.Inject

class LoginViewModel @Inject constructor(
    @ApplicationContext val context: Context
): ViewModel() {

    enum class AuthenticationStatus {
        SUCCESS, ERROR, LOADING
    }

    private val _authenticationStatus = MutableLiveData<AuthenticationStatus>()
    val authenticationStatus: LiveData<AuthenticationStatus> = _authenticationStatus

    private val _mageAccountStatus = MutableLiveData<AccountStatus>()
    val mageAccountStatus: LiveData<AccountStatus> = _mageAccountStatus

    fun mageLogin(credentials: Array<String>) {
        LoginTaskFactory.getInstance(context).getLoginTask( {
            _mageAccountStatus.value = it


            _authenticationStatus.value = if (it.status == AccountStatus.Status.SUCCESSFUL_LOGIN || it.status == AccountStatus.Status.DISCONNECTED_LOGIN ) {
                AuthenticationStatus.SUCCESS
            } else {
                AuthenticationStatus.ERROR
            }
        }, context).execute(*credentials)

        _mageAccountStatus.value = null
        _authenticationStatus.value = AuthenticationStatus.LOADING
    }

    private val _ldapAccountStatus = MutableLiveData<AccountStatus>()
    val ldapAccountStatus: LiveData<AccountStatus> = _ldapAccountStatus

    fun ldapLogin(credentials: Array<String>) {
        LdapLoginTask({
            _ldapAccountStatus.value = it

            _authenticationStatus.value = if (it.status == AccountStatus.Status.SUCCESSFUL_LOGIN || it.status == AccountStatus.Status.DISCONNECTED_LOGIN ) {
                AuthenticationStatus.SUCCESS
            } else {
                AuthenticationStatus.ERROR
            }
        }, context).execute(*credentials)

        _ldapAccountStatus.value = null
        _authenticationStatus.value = AuthenticationStatus.LOADING
    }

    private val _apiStatus = MutableLiveData<Boolean>()
    val apiStatus: LiveData<Boolean> = _apiStatus

    fun api(url: String) {
        if (StringUtils.isEmpty(url)) {
            return
        }

        val serverApi = ServerApi(context)
        serverApi.validateServerApi(url) { valid, _ ->
            if (authenticationStatus.value != AuthenticationStatus.LOADING) {
                _apiStatus.value = valid
            }
        }

        _apiStatus.value = null
    }

}