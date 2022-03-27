package mil.nga.giat.mage.login.ldap

import android.content.SharedPreferences
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import dagger.hilt.android.AndroidEntryPoint
import mil.nga.giat.mage.databinding.FragmentAuthenticationLdapBinding
import mil.nga.giat.mage.login.LoginViewModel
import org.json.JSONException
import org.json.JSONObject
import javax.inject.Inject

@AndroidEntryPoint
class LdapLoginFragment: Fragment() {

    companion object {
        private const val EXTRA_LDAP_STRATEGY = "EXTRA_LDAP_STRATEGY"
        private const val EXTRA_LDAP_STRATEGY_NAME = "EXTRA_LDAP_STRATEGY_NAME"

        fun newInstance(strategyName: String, strategy: JSONObject): LdapLoginFragment {
            return LdapLoginFragment().apply {
                arguments = Bundle().apply {
                    putString(EXTRA_LDAP_STRATEGY, strategy.toString())
                    putString(EXTRA_LDAP_STRATEGY_NAME, strategyName)
                }
            }
        }
    }

    private lateinit var binding: FragmentAuthenticationLdapBinding

    private lateinit var viewModel: LoginViewModel

    @Inject
    protected lateinit var preferences: SharedPreferences

    private lateinit var strategy: JSONObject
    private lateinit var strategyName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        require(arguments?.getString(EXTRA_LDAP_STRATEGY) != null) {"EXTRA_LDAP_STRATEGY is required to launch LdapLoginFragment"}
        require(arguments?.getString(EXTRA_LDAP_STRATEGY_NAME) != null) {"EXTRA_LDAP_STRATEGY_NAME is required to launch LdapLoginFragment"}

        strategy = JSONObject(requireArguments().getString(EXTRA_LDAP_STRATEGY)!!)
        strategyName = requireArguments().getString(EXTRA_LDAP_STRATEGY_NAME)!!
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel = activity?.run {
            ViewModelProvider(this).get(LoginViewModel::class.java)
        } ?: throw Exception("Invalid Activity")

        viewModel.authenticationStatus.observe(viewLifecycleOwner, { observeLogin(it) })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentAuthenticationLdapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.authenticationButton.bind(strategy)
        binding.authenticationButton.setOnClickListener { login() }

        try {
            val title = strategy.getString("title")
            binding.usernameLayout.hint = String.format("%s username", title)
            binding.passwordLayout.hint = String.format("%s password", title)
        } catch (ignore: JSONException) {}
    }

    private fun login() {
        binding.usernameLayout.error = null
        binding.passwordLayout.error = null

        val username = binding.username.text.toString()
        if (TextUtils.isEmpty(username)) {
            binding.usernameLayout.error = "Username cannot be blank"
            return
        }

        val password = binding.password.text.toString()
        if (TextUtils.isEmpty(password)) {
            binding.passwordLayout.error = "Password cannot be blank"
            return
        }

        viewModel.authenticate(strategyName, arrayOf(username, password))
    }

    private fun observeLogin(authentication: LoginViewModel.Authentication?) {
        if (authentication != null) {
            binding.usernameLayout.error = null
            binding.passwordLayout.error = null
        }
    }
}