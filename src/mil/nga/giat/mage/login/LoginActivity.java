package mil.nga.giat.mage.login;

import java.util.ArrayList;
import java.util.List;

import mil.nga.giat.mage.MainActivity;
import mil.nga.giat.mage.R;
import mil.nga.giat.mage.sdk.login.AccountDelegate;
import mil.nga.giat.mage.sdk.login.AccountStatus;
import mil.nga.giat.mage.sdk.login.LoginTaskFactory;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

public class LoginActivity extends Activity implements AccountDelegate {

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
		// load the configuration from preferences.xml
		PreferenceManager.setDefaultValues(getApplicationContext(), R.xml.preferences, true);
		// no title bar
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_login);
		hideKeyboardOnClick(findViewById(R.id.login));

		mUsernameEditText = (EditText) findViewById(R.id.login_username);
		mPasswordEditText = (EditText) findViewById(R.id.login_password);
		mServerEditText = (EditText) findViewById(R.id.login_server);

		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		getServerEditText().setText(sharedPreferences.getString("mServerEditText", ""));
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
		ImageView lockImageView = ((ImageView)findViewById(R.id.login_lock));
		if(lockImageView.getTag().toString().equals("lock")) {
			lockImageView.setTag("unlock");
			lockImageView.setImageResource(R.drawable.unlock_108);	
		} else {
			lockImageView.setTag("lock");
			lockImageView.setImageResource(R.drawable.lock_108);
		}
	}

	@Override
	public void finishAccount(AccountStatus accountStatus) {
		if (accountStatus.getStatus()) {
			Intent intent = new Intent(getApplicationContext(), MainActivity.class);
			startActivity(intent);
			finish();
		} else if (accountStatus.getErrorIndices().isEmpty()) {
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
