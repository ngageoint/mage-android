package mil.nga.giat.mage.login.mage

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_authentication_mage.*
import kotlinx.android.synthetic.main.fragment_authentication_mage.view.*
import mil.nga.giat.mage.R
import mil.nga.giat.mage.login.LoginListener
import mil.nga.giat.mage.login.LoginViewModel
import mil.nga.giat.mage.sdk.login.AccountStatus
import mil.nga.giat.mage.sdk.preferences.PreferenceHelper
import mil.nga.giat.mage.sdk.utils.PasswordUtility
import javax.inject.Inject

/**
 * Created by wnewman on 1/3/18.
 */

class MageLoginFragment : Fragment() {

    companion object {
        private val LOG_NAME = MageLoginFragment::class.java.name

        fun newInstance(listener: LoginListener): MageLoginFragment{
            val fragment = MageLoginFragment()
            fragment.loginListener = listener
            return fragment
        }
    }

    @Inject
    protected lateinit var preferences: SharedPreferences

    @Inject
    internal lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: LoginViewModel

    private var loginListener: LoginListener? = null

    private var currentUsername: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_authentication_mage, container, false)

        view.username.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (s?.isNotBlank() == true) {
                    view.username_layout.error = null
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

        })

        view.password.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (s?.isNotBlank() == true) {
                    view.password_layout.error = null
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        view.password.setOnKeyListener { _, keyCode, _ ->
            if (keyCode == KeyEvent.KEYCODE_ENTER) {
                login()
                true
            } else {
                false
            }
        }

        view.login_button.setOnClickListener { login() }

        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        AndroidSupportInjection.inject(this)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel = activity?.run {
            ViewModelProviders.of(this, viewModelFactory).get(LoginViewModel::class.java)
        } ?: throw Exception("Invalid Activity")

        viewModel.mageAccountStatus.observe(viewLifecycleOwner, Observer { observeLogin(it) })
    }

    private fun observeLogin(accountStatus: AccountStatus?) {
        if (accountStatus?.status == AccountStatus.Status.SUCCESSFUL_LOGIN || accountStatus?.status == AccountStatus.Status.DISCONNECTED_LOGIN) {
            val editor = preferences.edit()
            editor.putString(getString(R.string.usernameKey), username.text.toString()).apply()
            try {
                val hashedPassword = PasswordUtility.getSaltedHash(password.text.toString())
                editor.putString(getString(R.string.passwordHashKey), hashedPassword).commit()
            } catch (e: Exception) {
                Log.e(LOG_NAME, "Could not hash password", e)
            }

            val userChanged = currentUsername != preferences.getString(getString(mil.nga.giat.mage.sdk.R.string.usernameKey), "")
            if (accountStatus.status == AccountStatus.Status.DISCONNECTED_LOGIN) {
                AlertDialog.Builder(requireActivity())
                        .setTitle("Disconnected Login")
                        .setMessage("You are logging into MAGE in disconnected mode.  You must re-establish a connection in order to push and pull information to and from your server.")
                        .setPositiveButton(android.R.string.ok) { dialog, _ ->
                            dialog.dismiss()
                            loginListener?.onLoginComplete(userChanged)
                        }
                        .setCancelable(false)
                        .show()
            } else {
                loginListener?.onLoginComplete(userChanged)
            }
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

    fun login() {
        activity?.currentFocus?.let {
            val inputMethodManager = context?.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(it.windowToken, 0)
        }

        currentUsername = preferences.getString(getString(mil.nga.giat.mage.sdk.R.string.usernameKey), null)

        // reset errors
        username_layout.error = null
        password_layout.error = null

        val username = username.text.toString()
        val password = password.text.toString()

        if (username.isEmpty()) {
            username_layout.error = "Username cannot be blank"
            return
        }

        if (password.isEmpty()) {
            password_layout.error  = "Password cannot be blank"
            return
        }

        // if the username is different, then fallback to default preferences
        val oldUsername = preferences.getString(getString(R.string.usernameKey), null)
        if (oldUsername?.isNotEmpty() == true && oldUsername != username) {
            val preferenceHelper = PreferenceHelper.getInstance(context)
            preferenceHelper.initialize(true, mil.nga.giat.mage.sdk.R.xml::class.java, R.xml::class.java)

            val dayNightTheme = preferences.getInt(resources.getString(R.string.dayNightThemeKey), resources.getInteger(R.integer.dayNightThemeDefaultValue))
            AppCompatDelegate.setDefaultNightMode(dayNightTheme)
        }

        viewModel.mageLogin(arrayOf(username, password, false.toString()))
    }
}
