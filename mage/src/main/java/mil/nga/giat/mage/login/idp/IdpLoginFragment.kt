package mil.nga.giat.mage.login.idp

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import dagger.hilt.android.AndroidEntryPoint
import mil.nga.giat.mage.R
import mil.nga.giat.mage.databinding.FragmentAuthenticationIdpBinding
import mil.nga.giat.mage.login.LoginViewModel
import mil.nga.giat.mage.login.idp.IdpLoginActivity.Companion.EXTRA_IDP_TOKEN
import org.json.JSONObject
import javax.inject.Inject

@AndroidEntryPoint
class IdpLoginFragment : Fragment() {

   companion object {
      private const val EXTRA_IDP_RESULT = 100
      private const val EXTRA_IDP_STRATEGY = "EXTRA_IDP_STRATEGY"
      private const val EXTRA_IDP_STRATEGY_NAME = "EXTRA_IDP_STRATEGY_NAME"

      fun newInstance(strategyName: String, strategy: JSONObject): IdpLoginFragment {
         return IdpLoginFragment().apply {
            arguments = Bundle().apply {
               putString(EXTRA_IDP_STRATEGY, strategy.toString())
               putString(EXTRA_IDP_STRATEGY_NAME, strategyName)
            }
         }
      }
   }

   private lateinit var binding: FragmentAuthenticationIdpBinding

   private lateinit var viewModel: LoginViewModel

   @Inject
   lateinit var preferences: SharedPreferences

   private lateinit var strategy: JSONObject
   private lateinit var strategyName: String

   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)

      require(arguments?.getString(EXTRA_IDP_STRATEGY) != null) {"EXTRA_IDP_STRATEGY is required to launch IdpLoginFragment"}
      require(arguments?.getString(EXTRA_IDP_STRATEGY_NAME) != null) {"EXTRA_IDP_STRATEGY_NAME is required to launch IdpLoginFragment"}

      strategy = JSONObject(requireArguments().getString(EXTRA_IDP_STRATEGY)!!)
      strategyName = requireArguments().getString(EXTRA_IDP_STRATEGY_NAME)!!
   }

   override fun onActivityCreated(savedInstanceState: Bundle?) {
      super.onActivityCreated(savedInstanceState)

      viewModel = activity?.run {
         ViewModelProvider(this).get(LoginViewModel::class.java)
      } ?: throw Exception("Invalid Activity")
   }

   override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
      binding = FragmentAuthenticationIdpBinding.inflate(inflater, container, false)
      return binding.root
   }

   override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
      super.onViewCreated(view, savedInstanceState)

      binding.authenticationButton.bind(strategy)
      binding.authenticationButton.setOnClickListener { idpLogin() }
   }

   private fun idpLogin() {
      val serverUrl = preferences.getString(getString(R.string.serverURLKey), getString(R.string.serverURLDefaultValue))!!
      val intent = IdpLoginActivity.intent(context, serverUrl, strategyName)
      startActivityForResult(intent, EXTRA_IDP_RESULT)
   }

   override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
      if (requestCode == EXTRA_IDP_RESULT && resultCode == Activity.RESULT_OK) {
         val token = intent?.getStringExtra(EXTRA_IDP_TOKEN)
         authorize(token)
      }
   }

   private fun authorize(token: String?) {
      viewModel.authorize(strategyName, token ?: "")
   }
}