package mil.nga.giat.mage.login;

import java.util.ArrayList;
import java.util.List;

import mil.nga.giat.mage.R;
import android.app.Activity;
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

public class LoginActivity extends Activity {

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
				    inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
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

		LoginTaskFactory.getInstance(getApplicationContext()).getLoginTask(this).execute(credentials.toArray(new String[credentials.size()]));
	}
}
