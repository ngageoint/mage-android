package mil.nga.giat.mage.login.ldap

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_authentication_ldap.*
import kotlinx.android.synthetic.main.fragment_authentication_ldap.view.*
import mil.nga.giat.mage.R
import mil.nga.giat.mage.login.LoginListener
import mil.nga.giat.mage.login.LoginViewModel
import mil.nga.giat.mage.sdk.login.AccountStatus
import org.json.JSONException
import org.json.JSONObject
import javax.inject.Inject

class LdapLoginFragment: Fragment() {

    companion object {
        fun newInstance(strategy: JSONObject, listener: LoginListener): LdapLoginFragment {
            val fragment = LdapLoginFragment()
            fragment.strategy = strategy
            fragment.loginListener = listener
            return fragment
        }
    }

    @Inject
    internal lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: LoginViewModel

    @Inject
    protected lateinit var preferences: SharedPreferences

    private var strategy: JSONObject? = null
    private var loginListener: LoginListener? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel = activity?.run {
            ViewModelProviders.of(this, viewModelFactory).get(LoginViewModel::class.java)
        } ?: throw Exception("Invalid Activity")

        viewModel.ldapAccountStatus.observe(viewLifecycleOwner, Observer { observeLogin(it) })
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        AndroidSupportInjection.inject(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_authentication_ldap, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.authentication_button.bind(strategy)
        view.authentication_button.setOnClickListener { login() }

        try {
            val title = strategy?.getString("title")
            username_layout.hint = String.format("%s username", title)
            password_layout.hint = String.format("%s password", title)
        } catch (ignore: JSONException) {}
    }

    private fun observeLogin(accountStatus: AccountStatus?) {
        if (accountStatus?.status == AccountStatus.Status.SUCCESSFUL_LOGIN || accountStatus?.status == AccountStatus.Status.DISCONNECTED_LOGIN) {
            preferences.edit().putString(getString(R.string.usernameKey), username.text.toString()).apply()
            loginListener?.onLoginComplete()
        } else if (accountStatus?.status == AccountStatus.Status.SUCCESSFUL_REGISTRATION) {
            preferences.edit().putString(getString(R.string.usernameKey), username.text.toString()).apply()

            AlertDialog.Builder(requireActivity())
                    .setTitle("Registration Sent")
                    .setMessage(R.string.device_registered_text)
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
        } else {
            if (accountStatus?.status == AccountStatus.Status.INVALID_SERVER) {
                AlertDialog.Builder(requireActivity())
                        .setTitle("Application Compatibility Error")
                        .setMessage("This app is not compatible with this server. Please update your context or talk to your MAGE administrator.")
                        .setPositiveButton(android.R.string.ok) { dialog, _ -> dialog.dismiss() }.show()
            } else if (accountStatus?.errorIndices?.isEmpty() == true) {
                username_layout.error = null
                password_layout.error = null
                AlertDialog.Builder(requireActivity())
                        .setTitle("Incorrect Credentials")
                        .setMessage("The username or password you entered was incorrect.")
                        .setPositiveButton(android.R.string.ok) { dialog, _ -> dialog.dismiss() }.show()
                password_layout.requestFocus()
            } else {
                var errorMessageIndex = 0
                accountStatus?.errorIndices?.forEach {
                    var message = "Error"
                    if (errorMessageIndex < accountStatus.errorMessages.size) {
                        message = accountStatus.errorMessages[errorMessageIndex++]
                    }

                    when (it) {
                        0 -> {
                            username_layout.error = message
                            username_layout.requestFocus()
                        }
                        1 -> {
                            password_layout.error = message
                            password_layout.requestFocus()
                        }
                        2 -> {
                            AlertDialog.Builder(requireActivity())
                                    .setTitle("Login Failed")
                                    .setMessage(message)
                                    .setPositiveButton(android.R.string.ok, null)
                                    .show()
                        }
                    }
                }
            }
        }
    }

    private fun login() {
        username_layout.error = null
        password_layout.error = null

        val username = username.text.toString()
        if (TextUtils.isEmpty(username)) {
            username_layout.error = "Username cannot be blank"
            return
        }

        val password = password.text.toString()
        if (TextUtils.isEmpty(password)) {
            password_layout.error = "Password cannot be blank"
            return
        }

        viewModel.ldapLogin(arrayOf(username, password))
    }
}