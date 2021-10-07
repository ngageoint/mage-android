package mil.nga.giat.mage.login.mage

import android.app.Activity
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_authentication_mage.*
import kotlinx.android.synthetic.main.fragment_authentication_mage.view.*
import mil.nga.giat.mage.R
import mil.nga.giat.mage.login.LoginViewModel
import org.json.JSONObject
import java.util.*
import javax.inject.Inject

/**
 * Created by wnewman on 1/3/18.
 */

@AndroidEntryPoint
class MageLoginFragment : Fragment() {

    companion object {
        private const val EXTRA_LOCAL_STRATEGY = "EXTRA_LOCAL_STRATEGY"
        private const val EXTRA_LOCAL_STRATEGY_NAME = "EXTRA_LOCAL_STRATEGY_NAME"

        fun newInstance(strategyName: String, strategy: JSONObject): MageLoginFragment {
            return MageLoginFragment().apply {
                arguments = Bundle().apply {
                    putString(EXTRA_LOCAL_STRATEGY, strategy.toString())
                    putString(EXTRA_LOCAL_STRATEGY_NAME, strategyName)
                }
            }
        }
    }

    @Inject
    protected lateinit var preferences: SharedPreferences

    private lateinit var viewModel: LoginViewModel

    private lateinit var strategy: JSONObject
    private lateinit var strategyName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        require(arguments?.getString(EXTRA_LOCAL_STRATEGY) != null) {"EXTRA_LOCAL_STRATEGY is required to launch MageLoginFragment"}
        require(arguments?.getString(EXTRA_LOCAL_STRATEGY_NAME) != null) {"EXTRA_LOCAL_STRATEGY_NAME is required to launch MageLoginFragment"}

        strategy = JSONObject(requireArguments().getString(EXTRA_LOCAL_STRATEGY)!!)
        strategyName = requireArguments().getString(EXTRA_LOCAL_STRATEGY_NAME)!!
    }

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

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel = activity?.run {
            ViewModelProviders.of(this).get(LoginViewModel::class.java)
        } ?: throw Exception("Invalid Activity")

        viewModel.authenticationStatus.observe(viewLifecycleOwner, Observer { observeLogin(it) })
    }

    fun login() {
        activity?.currentFocus?.let {
            val inputMethodManager = context?.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(it.windowToken, 0)
        }

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

        viewModel.authenticate(strategyName, arrayOf(username.toLowerCase(Locale.getDefault()), password), true)
    }

    private fun observeLogin(authentication: LoginViewModel.Authentication?) {
        if (authentication != null) {
            username_layout.error = null
            password_layout.error = null
        }
    }
}
