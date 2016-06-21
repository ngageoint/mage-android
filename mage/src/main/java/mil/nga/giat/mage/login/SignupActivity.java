package mil.nga.giat.mage.login;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.sdk.login.AccountDelegate;
import mil.nga.giat.mage.sdk.login.AccountStatus;
import mil.nga.giat.mage.sdk.login.SignupTask;
import mil.nga.giat.mage.sdk.preferences.PreferenceHelper;

/**
 * The signup screen
 *
 * @author wiedemanns
 */
public class SignupActivity extends Activity implements AccountDelegate {

	public static final int EXTRA_OAUTH_RESULT = 1;

	public static final String EXTRA_OAUTH_ERROR = "OAUTH_ERROR";
	public static final String EXTRA_OAUTH_ERROR_MESSAGE = "OAUTH_ERROR_MESSAGE";

	private static final String LOG_NAME = SignupActivity.class.getName();

	private EditText mDisplayNameEditText;
	private EditText mUsernameEditText;
	private EditText mEmailEditText;
	private EditText mPasswordEditText;
	private EditText mConfirmPasswordEditText;

	private String serverURL;

	public final EditText getDisplayNameEditText() {
		return mDisplayNameEditText;
	}

	public final EditText getUsernameEditText() {
		return mUsernameEditText;
	}

	public final EditText getEmailEditText() {
		return mEmailEditText;
	}

	public final EditText getPasswordEditText() {
		return mPasswordEditText;
	}

	public final EditText getConfirmPasswordEditText() {
		return mConfirmPasswordEditText;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// no title bar
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_signup);
		hideKeyboardOnClick(findViewById(R.id.signup));

		mDisplayNameEditText = (EditText) findViewById(R.id.signup_displayname);
		mUsernameEditText = (EditText) findViewById(R.id.signup_username);
		mEmailEditText = (EditText) findViewById(R.id.signup_email);
		mPasswordEditText = (EditText) findViewById(R.id.signup_password);
		mPasswordEditText.setTypeface(Typeface.DEFAULT);
		mConfirmPasswordEditText = (EditText) findViewById(R.id.signup_confirmpassword);
		mConfirmPasswordEditText.setTypeface(Typeface.DEFAULT);

		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		serverURL = sharedPreferences.getString(getString(R.string.serverURLKey), getString(R.string.serverURLDefaultValue));

		mEmailEditText.setOnKeyListener(new View.OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_ENTER) {
					signup(v);
					return true;
				} else {
					return false;
				}
			}
		});

		configureSignup();
	}

	private void configureSignup() {
		PreferenceHelper preferenceHelper = PreferenceHelper.getInstance(getApplicationContext());
		if (preferenceHelper.containsLocalAuthentication()) {
			Button localButton = (Button) findViewById(R.id.local_signup_button);
			localButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					signup(v);
				}
			});
		}

		if (preferenceHelper.containsGoogleAuthentication()) {
			Button googleButton = (Button) findViewById(R.id.google_signup_button);
			googleButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					googleSignup();
				}
			});
		}
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
		startActivity(new Intent(getApplicationContext(), LoginActivity.class));
		finish();
	}

	private void googleSignup() {
		Intent intent = new Intent(getApplicationContext(), OAuthActivity.class);
		intent.putExtra(OAuthActivity.EXTRA_SERVER_URL, serverURL);
		intent.putExtra(OAuthActivity.EXTRA_OAUTH_URL, serverURL + "/auth/google/signup");
		intent.putExtra(OAuthActivity.EXTRA_OAUTH_TYPE, OAuthActivity.OAuthType.SIGINUP);
		startActivityForResult(intent, EXTRA_OAUTH_RESULT);
	}

	/**
	 * Fired when user clicks signup
	 *
	 * @param view
	 */
	public void signup(View view) {
		// reset errors
		getDisplayNameEditText().setError(null);
		getUsernameEditText().setError(null);
		getEmailEditText().setError(null);
		getPasswordEditText().setError(null);
		getConfirmPasswordEditText().setError(null);

		String displayName = getDisplayNameEditText().getText().toString();
		String username = getUsernameEditText().getText().toString();
		String email = getEmailEditText().getText().toString();
		String password = getPasswordEditText().getText().toString();
		String confirmpassword = getConfirmPasswordEditText().getText().toString();

		// are the inputs valid?
		if (TextUtils.isEmpty(displayName)) {
			getDisplayNameEditText().setError("Display name can not be blank");
			getDisplayNameEditText().requestFocus();
			return;
		}

		if (TextUtils.isEmpty(username)) {
			getUsernameEditText().setError("Username can not be blank");
			getUsernameEditText().requestFocus();
			return;
		}

		if (TextUtils.isEmpty(email)) {
			getEmailEditText().setError("Email can not be blank");
			getEmailEditText().requestFocus();
			return;
		}

		// is email address the right syntax?
		if (StringUtils.isNotEmpty(email) && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
			getEmailEditText().setError("Not an Email address");
			getEmailEditText().requestFocus();
			return;
		}

		if (TextUtils.isEmpty(password)) {
			getPasswordEditText().setError("Password can not be blank");
			getPasswordEditText().requestFocus();
			return;
		}

		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		Integer passwordLength = sharedPreferences.getInt(getString(R.string.passwordMinLengthKey), getResources().getInteger(R.integer.passwordMinLengthDefaultValue));
		if (password.length() < passwordLength) {
			getPasswordEditText().setError("Password must be " + passwordLength + " characters");
			getPasswordEditText().requestFocus();
			return;
		}

		if (TextUtils.isEmpty(confirmpassword)) {
			getConfirmPasswordEditText().setError("Enter password again");
			getConfirmPasswordEditText().requestFocus();
			return;
		}

		// do passwords match?
		if (!password.equals(confirmpassword)) {
			getPasswordEditText().setError("Passwords do not match");
			getConfirmPasswordEditText().setError("Passwords do not match");
			getConfirmPasswordEditText().requestFocus();
			return;
		}

		List<String> accountInfo = new ArrayList<>();
		accountInfo.add(displayName);
		accountInfo.add(username);
		accountInfo.add(email);
		accountInfo.add(password);
		accountInfo.add(serverURL);

		// show spinner, and hide form
		findViewById(R.id.signup_form).setVisibility(View.GONE);
		findViewById(R.id.signup_status).setVisibility(View.VISIBLE);

		new SignupTask(this, this.getApplicationContext()).execute(accountInfo.toArray(new String[accountInfo.size()]));
	}

	private void showKeyboard() {
		InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
		inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0);
	}

	@Override
	public void finishAccount(AccountStatus accountStatus) {
		if (accountStatus.getStatus() == AccountStatus.Status.SUCCESSFUL_SIGNUP) {

			// we might be able to set the username for the user
			SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
			String oldUsername = sharedPreferences.getString(getString(mil.nga.giat.mage.sdk.R.string.usernameKey), getString(mil.nga.giat.mage.sdk.R.string.usernameDefaultValue));
			Editor sp = sharedPreferences.edit();
			if (TextUtils.isEmpty(oldUsername)) {
				try {
					sp.putString(getApplicationContext().getString(R.string.usernameKey), accountStatus.getAccountInformation().get("username").getAsString()).commit();
				} catch (Exception e) {
					Log.w(LOG_NAME, "Unable to save username");
				}
			}

			// Tell the user that their account was made
			showSignupSuccessDialog();
		} else {
			if (accountStatus.getErrorIndices().isEmpty()) {
				getUsernameEditText().requestFocus();
			} else {
				int errorMessageIndex = 0;
				for (Integer errorIndex : accountStatus.getErrorIndices()) {
					String message = "Error";
					if (errorMessageIndex < accountStatus.getErrorMessages().size()) {
						message = accountStatus.getErrorMessages().get(errorMessageIndex++);
					}
					if (errorIndex == 0) {
						getDisplayNameEditText().setError(message);
						getDisplayNameEditText().requestFocus();
					} else if (errorIndex == 1) {
						getUsernameEditText().setError(message);
						getUsernameEditText().requestFocus();
					} else if (errorIndex == 2) {
						getEmailEditText().setError(message);
						getEmailEditText().requestFocus();
					} else if (errorIndex == 3) {
						getPasswordEditText().setError(message);
						getPasswordEditText().requestFocus();
					} else if (errorIndex == 5) {
						new AlertDialog.Builder(this)
							.setTitle("Signup Failed")
							.setMessage(message)
							.setPositiveButton(android.R.string.ok, null)
							.show();
					}
				}
			}
			// show form, and hide spinner
			findViewById(R.id.signup_status).setVisibility(View.GONE);
			findViewById(R.id.signup_form).setVisibility(View.VISIBLE);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		// show form, and hide spinner
		findViewById(R.id.signup_status).setVisibility(View.GONE);
		findViewById(R.id.signup_form).setVisibility(View.VISIBLE);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (requestCode == EXTRA_OAUTH_RESULT && resultCode == RESULT_OK) {
			if (intent.getBooleanExtra(EXTRA_OAUTH_ERROR, false)) {
				String errorMessage = intent.getStringExtra(EXTRA_OAUTH_ERROR_MESSAGE);
				AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
				alertDialog.setTitle("Problem Creating Account");
				alertDialog.setMessage(errorMessage);
				alertDialog.setPositiveButton("Ok", null);
				alertDialog.show();
			} else {
				showSignupSuccessDialog();
			}
		}
	}

	private void showSignupSuccessDialog() {
		AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
		alertDialog.setTitle("Account Created");
		alertDialog.setMessage("Your account has been created but it is not enabled.  An administrator needs to enable your account before you can log in.");
		alertDialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				login(null);
			}
		});
		alertDialog.show();
	}
}
