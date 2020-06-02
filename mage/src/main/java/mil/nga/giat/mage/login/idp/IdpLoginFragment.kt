package mil.nga.giat.mage.login.idp

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_authentication_idp.*
import mil.nga.giat.mage.R
import mil.nga.giat.mage.login.LoginViewModel
import mil.nga.giat.mage.login.idp.IdpLoginActivity.Companion.EXTRA_IDP_TOKEN
import org.json.JSONObject
import javax.inject.Inject

class IdpLoginFragment : Fragment() {

    companion object {
        private const val EXTRA_IDP_RESULT = 100

        fun newInstance(strategyName: String, strategy: JSONObject): IdpLoginFragment {
            val fragment = IdpLoginFragment()
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

    private lateinit var strategyName: String
    private var strategy: JSONObject? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel = activity?.run {
            ViewModelProviders.of(this, viewModelFactory).get(LoginViewModel::class.java)
        } ?: throw Exception("Invalid Activity")
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        AndroidSupportInjection.inject(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_authentication_idp, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

		authentication_button.bind(strategy)
        authentication_button.setOnClickListener { idpLogin(strategyName) }
    }

    private fun idpLogin(strategy: String?) {
        val serverUrl = preferences.getString(getString(R.string.serverURLKey), getString(R.string.serverURLDefaultValue))
        val intent = IdpLoginActivity.intent(context, serverUrl, strategy)
        startActivityForResult(intent, EXTRA_IDP_RESULT)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (requestCode == EXTRA_IDP_RESULT && resultCode == Activity.RESULT_OK) {
            val token = intent?.getStringExtra(EXTRA_IDP_TOKEN)
            authorize(token)
        }
    }

    private fun authorize(token: String?) {
        viewModel.authorize(strategyName,  token ?: "")
    }
}