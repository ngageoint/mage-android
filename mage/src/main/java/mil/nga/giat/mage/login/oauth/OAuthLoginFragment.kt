package mil.nga.giat.mage.login.oauth

import android.app.Activity
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_authentication_oauth.*
import mil.nga.giat.mage.R
import mil.nga.giat.mage.login.LoginViewModel
import org.json.JSONObject
import javax.inject.Inject

class OAuthLoginFragment : Fragment() {

    companion object {
        private val EXTRA_OAUTH_RESULT = 1

        val EXTRA_OAUTH_UNREGISTERED_DEVICE = "OAUTH_UNREGISTERED_DEVICE"

        fun newInstance(strategyName: String, strategy: JSONObject): OAuthLoginFragment {
            val fragment = OAuthLoginFragment()
            fragment.strategyName = strategyName
            fragment.strategy = strategy
            return fragment
        }
    }

    @Inject
    internal lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: LoginViewModel

    @Inject
    protected lateinit var preferences: SharedPreferences

    private var strategyName: String? = null
    private var strategy: JSONObject? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel = activity?.run {
            ViewModelProviders.of(this, viewModelFactory).get(LoginViewModel::class.java)
        } ?: throw Exception("Invalid Activity")
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        AndroidSupportInjection.inject(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_authentication_oauth, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

		authentication_button.bind(strategy)
        authentication_button.setOnClickListener { oauthLogin(strategyName) }
    }

    private fun oauthLogin(strategy: String?) {
        val serverUrl = preferences.getString(getString(R.string.serverURLKey), getString(R.string.serverURLDefaultValue))

        val intent = Intent(context, OAuthLoginActivity::class.java)
        intent.putExtra(OAuthLoginActivity.EXTRA_SERVER_URL, serverUrl)
        intent.putExtra(OAuthLoginActivity.EXTRA_OAUTH_TYPE, OAuthLoginActivity.OAuthType.SIGNIN)
        intent.putExtra(OAuthLoginActivity.EXTRA_OAUTH_STRATEGY, strategy)
        startActivityForResult(intent, EXTRA_OAUTH_RESULT)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (requestCode == EXTRA_OAUTH_RESULT && resultCode == Activity.RESULT_OK) {
            if (intent?.getBooleanExtra(EXTRA_OAUTH_UNREGISTERED_DEVICE, false) == true) {
                showUnregisteredDeviceDialog()
            } else {
                showOAuthErrorDialog()
            }
        }
    }

    private fun showUnregisteredDeviceDialog() {
        AlertDialog.Builder(requireActivity())
                .setTitle("Registration Sent")
                .setMessage(R.string.device_registered_text)
                .setPositiveButton(android.R.string.ok, null)
                .show()
    }

    private fun showOAuthErrorDialog() {
        AlertDialog.Builder(requireActivity())
                .setTitle("Inactive MAGE Account")
                .setMessage("Please contact a MAGE administrator to activate your account.")
                .setPositiveButton(android.R.string.ok, null)
                .show()
    }
}