package mil.nga.giat.mage.login.ldap

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_authentication_ldap.*
import kotlinx.android.synthetic.main.fragment_authentication_ldap.view.*
import mil.nga.giat.mage.R
import mil.nga.giat.mage.login.LoginViewModel
import org.json.JSONException
import org.json.JSONObject
import javax.inject.Inject

class LdapLoginFragment: Fragment() {

    companion object {
        private const val STRATEGY_NAME = "ldap"

        fun newInstance(strategy: JSONObject): LdapLoginFragment {
            val fragment = LdapLoginFragment()
            fragment.strategy = strategy
            return fragment
        }
    }

    @Inject
    internal lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: LoginViewModel

    @Inject
    protected lateinit var preferences: SharedPreferences

    private var strategy: JSONObject? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel = activity?.run {
            ViewModelProviders.of(this, viewModelFactory).get(LoginViewModel::class.java)
        } ?: throw Exception("Invalid Activity")

        viewModel.authenticationStatus.observe(viewLifecycleOwner, Observer { observeLogin(it) })
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

        viewModel.authenticate(STRATEGY_NAME, arrayOf(username, password, STRATEGY_NAME))
    }

    private fun observeLogin(authentication: LoginViewModel.Authentication?) {
        if (authentication != null) {
            username_layout.error = null
            password_layout.error = null
        }
    }
}