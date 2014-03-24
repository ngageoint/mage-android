package mil.nga.giat.mage.login;

import java.util.ArrayList;
import java.util.List;

import mil.nga.giat.mage.LandingActivity;
import mil.nga.giat.mage.R;
import mil.nga.giat.mage.disclaimer.DisclaimerActivity;
import mil.nga.giat.mage.sdk.login.AccountDelegate;
import mil.nga.giat.mage.sdk.login.AccountStatus;
import mil.nga.giat.mage.sdk.login.LoginTaskFactory;
import mil.nga.giat.mage.sdk.preferences.PreferenceHelper;
import mil.nga.giat.mage.sdk.utils.UserUtility;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.text.InputType;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;

/**
 * The login screen
 * 
 * @author wiedemannse
 *
 */
public class LoginActivity extends FragmentActivity implements AccountDelegate {

	private EditText mUsernameEditText;
	private EditText mPasswordEditText;
	private EditText mServerEditText;

	public final EditText getUsernameEditText() {
		return mUsernameEditText;
	}

	public final EditText getPasswordEditText() {
		return mPasswordEditText;
	}

	public final EditText getServerEditText() {
		return mServerEditText;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// IMPORTANT: load the configuration from preferences files and server
		PreferenceHelper.getInstance(getApplicationContext()).initializeAll(new int[]{R.xml.privatepreferences, R.xml.publicpreferences});
		
		// show the disclaimer?
		if (PreferenceHelper.getInstance(getApplicationContext()).getValue(R.string.showDisclaimerKey, Boolean.class, Boolean.TRUE)) {
			Intent intent = new Intent(this, DisclaimerActivity.class);
			startActivity(intent);
		}
		
		// if token is not expired, then skip the login module
		if (!UserUtility.getInstance(getApplicationContext()).isTokenExpired()) {
			startActivity(new Intent(getApplicationContext(), LandingActivity.class));
		}
		
		// no title bar
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_login);
		hideKeyboardOnClick(findViewById(R.id.login));

		mUsernameEditText = (EditText) findViewById(R.id.login_username);
		mPasswordEditText = (EditText) findViewById(R.id.login_password);
		mServerEditText = (EditText) findViewById(R.id.login_server);

		// set the default values
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		getUsernameEditText().setText(sharedPreferences.getString("username", ""));
		getUsernameEditText().setSelection(getUsernameEditText().getText().length());
		getServerEditText().setText(sharedPreferences.getString("serverURL", ""));
		getServerEditText().setSelection(getServerEditText().getText().length());
	}

	public void togglePassword(View v) {
		CheckBox c = (CheckBox)v;
		EditText pw = (EditText)findViewById(R.id.login_password);
		if (c.isChecked()) {
			pw.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
		} else {
			pw.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
		}
		pw.setSelection(pw.getText().length());
	}

	/**
	 * Hides keyboard when clicking elsewhere
	 * 
	 * @param view
	 */
	private void hideKeyboardOnClick(View view) {
		// Set up touch listener for non-text box views to hide keyboard.
		if (!(view instanceof EditText) && !(view instanceof Button)) {
			view.setOnTouchListener(new OnTouchListener() {
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
					if (getCurrentFocus() != null) {
						inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
					}
					return false;
				}
			});
		}

		// If a layout container, iterate over children and seed recursion.
		if (view instanceof ViewGroup) {
			for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
				View innerView = ((ViewGroup) view).getChildAt(i);
				hideKeyboardOnClick(innerView);
			}
		}
	}

	/**
	 * Fired when user clicks login
	 * 
	 * @param view
	 */
	public void login(View view) {
		// reset errors
		getUsernameEditText().setError(null);
		getPasswordEditText().setError(null);
		getServerEditText().setError(null);

		String username = getUsernameEditText().getText().toString();
		String password = getPasswordEditText().getText().toString();
		String server = getServerEditText().getText().toString();

		// are the inputs valid?
		if (TextUtils.isEmpty(username)) {
			getUsernameEditText().setError("Username can not be blank");
			getUsernameEditText().requestFocus();
			return;
		}

		if (TextUtils.isEmpty(password)) {
			getPasswordEditText().setError("Password can not be blank");
			getPasswordEditText().requestFocus();
			return;
		}

		List<String> credentials = new ArrayList<String>();
		credentials.add(username);
		credentials.add(password);
		credentials.add(server);

		// show spinner, and hide form
		findViewById(R.id.login_form).setVisibility(View.GONE);
		findViewById(R.id.login_status).setVisibility(View.VISIBLE);

		LoginTaskFactory.getInstance(getApplicationContext()).getLoginTask(this, this.getApplicationContext()).execute(credentials.toArray(new String[credentials.size()]));
	}

	/**
	 * Fired when user clicks signup
	 * 
	 * @param view
	 */
	public void signup(View view) {
		Intent intent = new Intent(getApplicationContext(), SignupActivity.class);
		startActivity(intent);
		finish();
	}

	/**
	 * Fired when user clicks lock
	 * 
	 * @param view
	 */
	public void toggleLock(View view) {
		getServerEditText().setEnabled(!getServerEditText().isEnabled());
		ImageView lockImageView = ((ImageView) findViewById(R.id.login_lock));
		if (lockImageView.getTag().toString().equals("lock")) {
			lockImageView.setTag("unlock");
			lockImageView.setImageResource(R.drawable.ic_unlock_white);
			InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
			inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
			getServerEditText().requestFocus();
		} else {
			lockImageView.setTag("lock");
			lockImageView.setImageResource(R.drawable.ic_lock_white);
		}
	}

	@Override
	public void finishAccount(AccountStatus accountStatus) {
		if (accountStatus.getStatus()) {
			Editor sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
			sp.putString("username", getUsernameEditText().getText().toString());
			// TODO should we store password, or some hash?
//			sp.putString("password", getPasswordEditText().getText().toString());
			sp.putString("serverURL", getServerEditText().getText().toString());
			sp.commit();
			startActivity(new Intent(getApplicationContext(), LandingActivity.class));
		} else {
			if (accountStatus.getErrorIndices().isEmpty()) {
				getUsernameEditText().setError("Check your username");
				getPasswordEditText().setError("Check your password");
				getUsernameEditText().requestFocus();
			} else {
				int errorMessageIndex = 0;
				for (Integer errorIndex : accountStatus.getErrorIndices()) {
					String message = "Error";
					if (errorMessageIndex < accountStatus.getErrorMessages().size()) {
						message = accountStatus.getErrorMessages().get(errorMessageIndex++);
					}
					if (errorIndex == 0) {
						getUsernameEditText().setError(message);
						getUsernameEditText().requestFocus();
					} else if (errorIndex == 1) {
						getPasswordEditText().setError(message);
						getPasswordEditText().requestFocus();
					} else if (errorIndex == 2) {
						getServerEditText().setError(message);
						getServerEditText().requestFocus();
					}
				}
			}
			// show form, and hide spinner
			findViewById(R.id.login_status).setVisibility(View.GONE);
			findViewById(R.id.login_form).setVisibility(View.VISIBLE);
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		// TODO : populate username and password from preferences
		
		// show form, and hide spinner
		findViewById(R.id.login_status).setVisibility(View.GONE);
		findViewById(R.id.login_form).setVisibility(View.VISIBLE);
	}
}
