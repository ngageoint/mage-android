package mil.nga.giat.mage.login;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.sdk.connectivity.ConnectivityUtility;
import mil.nga.giat.mage.sdk.login.AccountDelegate;
import mil.nga.giat.mage.sdk.login.AccountStatus;
import mil.nga.giat.mage.sdk.login.SignupTask;
import mil.nga.giat.mage.sdk.preferences.PreferenceHelper;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
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
import android.widget.ImageView;

import com.google.common.base.Predicate;

/**
 * The signup screen
 *
 * @author wiedemanns
 */
public class SignupActivity extends Activity implements AccountDelegate {

	private static final String LOG_NAME = SignupActivity.class.getName();

	private EditText mFirstNameEditText;
	private EditText mLastNameEditText;
	private EditText mUsernameEditText;
	private EditText mEmailEditText;
	private EditText mPasswordEditText;
	private EditText mConfirmPasswordEditText;
	private EditText mServerEditText;
	private Button mSignupButton;

	public final EditText getFirstNameEditText() {
		return mFirstNameEditText;
	}

	public final EditText getLastNameEditText() {
		return mLastNameEditText;
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

	public final EditText getServerEditText() {
		return mServerEditText;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// no title bar
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_signup);
		hideKeyboardOnClick(findViewById(R.id.signup));

		mFirstNameEditText = (EditText) findViewById(R.id.signup_firstname);
		mLastNameEditText = (EditText) findViewById(R.id.signup_lastname);
		mUsernameEditText = (EditText) findViewById(R.id.signup_username);
		mEmailEditText = (EditText) findViewById(R.id.signup_email);
		mPasswordEditText = (EditText) findViewById(R.id.signup_password);
		mPasswordEditText.setTypeface(Typeface.DEFAULT);
		mConfirmPasswordEditText = (EditText) findViewById(R.id.signup_confirmpassword);
		mConfirmPasswordEditText.setTypeface(Typeface.DEFAULT);
		mServerEditText = (EditText) findViewById(R.id.signup_server);

		mSignupButton = (Button) findViewById(R.id.signup_signup_button);

		// generate the username from first and lastname
		TextWatcher firstnameWatcher = new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}

			@Override
			public void afterTextChanged(Editable s) {
				String firstname = s.toString();
				String lastname = mLastNameEditText.getText().toString();
				String username = (lastname + ((firstname.length() > 0) ? firstname.substring(0, 1) : "")).toLowerCase(Locale.getDefault());
				mUsernameEditText.setText(username);
				mUsernameEditText.setSelection(username.length());
			}
		};

		TextWatcher lastnameWatcher = new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}

			@Override
			public void afterTextChanged(Editable s) {
				String firstname = mFirstNameEditText.getText().toString();
				String lastname = s.toString();
				String username = (lastname + ((firstname.length() > 0) ? firstname.substring(0, 1) : "")).toLowerCase(Locale.getDefault());
				mUsernameEditText.setText(username);
				mUsernameEditText.setSelection(username.length());
			}
		};

		mFirstNameEditText.addTextChangedListener(firstnameWatcher);
		mLastNameEditText.addTextChangedListener(lastnameWatcher);

		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		getServerEditText().setText(sharedPreferences.getString("serverURL", ""));
		getServerEditText().setSelection(getServerEditText().getText().length());

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

	/**
	 * Fired when user clicks signup
	 *
	 * @param view
	 */
	public void signup(View view) {
		// reset errors
		getFirstNameEditText().setError(null);
		getLastNameEditText().setError(null);
		getUsernameEditText().setError(null);
		getEmailEditText().setError(null);
		getPasswordEditText().setError(null);
		getConfirmPasswordEditText().setError(null);
		getServerEditText().setError(null);

		String firstname = getFirstNameEditText().getText().toString();
		String lastname = getLastNameEditText().getText().toString();
		String username = getUsernameEditText().getText().toString();
		String email = getEmailEditText().getText().toString();
		String password = getPasswordEditText().getText().toString();
		String confirmpassword = getConfirmPasswordEditText().getText().toString();
		String server = getServerEditText().getText().toString();

		// are the inputs valid?
		if (TextUtils.isEmpty(firstname)) {
			getFirstNameEditText().setError("First name can not be blank");
			getFirstNameEditText().requestFocus();
			return;
		}

		if (TextUtils.isEmpty(lastname)) {
			getLastNameEditText().setError("Last name can not be blank");
			getLastNameEditText().requestFocus();
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
		if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
			getEmailEditText().setError("Not an Email address");
			getEmailEditText().requestFocus();
			return;
		}

		if (TextUtils.isEmpty(password)) {
			getPasswordEditText().setError("Password can not be blank");
			getPasswordEditText().requestFocus();
			return;
		}

		Long passwordLength = PreferenceHelper.getInstance(getApplicationContext()).getValue(R.string.passwordMinLengthKey, Long.class, R.string.passwordMinLengthDefaultValue);
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

		List<String> accountInfo = new ArrayList<String>();
		accountInfo.add(firstname);
		accountInfo.add(lastname);
		accountInfo.add(username);
		accountInfo.add(email);
		accountInfo.add(password);
		accountInfo.add(server);

		// show spinner, and hide form
		findViewById(R.id.signup_form).setVisibility(View.GONE);
		findViewById(R.id.signup_status).setVisibility(View.VISIBLE);

		new SignupTask(this, this.getApplicationContext()).execute(accountInfo.toArray(new String[accountInfo.size()]));
	}

	/**
	 * Fired when user clicks lock
	 *
	 * @param view
	 */
	public void toggleLock(View view) {
		final ImageView lockImageView = ((ImageView) findViewById(R.id.signup_lock));
		if (lockImageView.getTag().toString().equals("lock")) {
			getServerEditText().setEnabled(!getServerEditText().isEnabled());
			mSignupButton.setEnabled(!mSignupButton.isEnabled());
			lockImageView.setTag("unlock");
			lockImageView.setImageResource(R.drawable.unlock_108);
			showKeyboard();
			getServerEditText().requestFocus();
		} else {
			final View serverProgress = findViewById(R.id.signup_server_progress);
			try {
				lockImageView.setVisibility(View.GONE);
				serverProgress.setVisibility(View.VISIBLE);

				// make sure the url syntax is good
				String serverURL = getServerEditText().getText().toString();
				final URL sURL = new URL(serverURL);

				// make sure you can get to the host!
				ConnectivityUtility.isResolvable(sURL.getHost(), new Predicate<Exception>() {
					@Override
					public boolean apply(Exception e) {
						if (e == null) {
							PreferenceHelper.getInstance(getApplicationContext()).readRemoteApi(sURL, new Predicate<Exception>() {
								public boolean apply(Exception e) {
									getServerEditText().setError(null);
									serverProgress.setVisibility(View.GONE);
									lockImageView.setVisibility(View.VISIBLE);
									if (e != null) {
										showKeyboard();
										getServerEditText().setError("No server information");
										getServerEditText().requestFocus();
										return false;
									} else {
										// check versions
										Integer compatibleMajorVersion = PreferenceHelper.getInstance(getApplicationContext()).getValue(R.string.compatibleVersionMajorKey, Integer.class, R.string.compatibleVersionMajorDefaultValue);
										Integer compatibleMinorVersion = PreferenceHelper.getInstance(getApplicationContext()).getValue(R.string.compatibleVersionMinorKey, Integer.class, R.string.compatibleVersionMinorDefaultValue);

										Integer serverMajorVersion = PreferenceHelper.getInstance(getApplicationContext()).getValue(R.string.serverVersionMajorKey, Integer.class, null);
										Integer serverMinorVersion = PreferenceHelper.getInstance(getApplicationContext()).getValue(R.string.serverVersionMinorKey, Integer.class, null);

										if (serverMajorVersion == null || serverMinorVersion == null) {
											showKeyboard();
											getServerEditText().setError("No server version");
											getServerEditText().requestFocus();
										} else {
											if (!compatibleMajorVersion.equals(serverMajorVersion)) {
												showKeyboard();
												getServerEditText().setError("This app is not compatible with this server");
												getServerEditText().requestFocus();
											} else if (compatibleMinorVersion > serverMinorVersion) {
												showKeyboard();
												getServerEditText().setError("This app is not compatible with this server");
												getServerEditText().requestFocus();
											} else {
												getServerEditText().setEnabled(!getServerEditText().isEnabled());
												mSignupButton.setEnabled(!mSignupButton.isEnabled());
												lockImageView.setTag("lock");
												lockImageView.setImageResource(R.drawable.lock_108);
											}
										}
									}
									return true;
								}
							});
						} else {
							showKeyboard();
							getServerEditText().setError("Host does not resolve");
							getServerEditText().requestFocus();
							serverProgress.setVisibility(View.GONE);
							lockImageView.setVisibility(View.VISIBLE);
							return false;
						}
						return true;
					}
				});
			} catch (MalformedURLException mue) {
				showKeyboard();
				getServerEditText().setError("Bad URL");
				getServerEditText().requestFocus();
				serverProgress.setVisibility(View.GONE);
				lockImageView.setVisibility(View.VISIBLE);
			}
		}
	}

	private void showKeyboard() {
		InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
		inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0);
	}

	@Override
	public void finishAccount(AccountStatus accountStatus) {
		if (accountStatus.getStatus() == AccountStatus.Status.SUCCESSFUL_SIGNUP) {

			// we might be able to set the username for the user
			String oldUsername = PreferenceHelper.getInstance(getApplicationContext()).getValue(mil.nga.giat.mage.sdk.R.string.usernameKey);
			Editor sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
			if (TextUtils.isEmpty(oldUsername)) {
				try {
					sp.putString(getApplicationContext().getString(R.string.usernameKey), accountStatus.getAccountInformation().getString("username")).commit();
				} catch (Exception e) {
					Log.w(LOG_NAME, "unabel to save username");
				}
			}

			sp.putString(getApplicationContext().getString(R.string.serverURLKey), getServerEditText().getText().toString()).commit();
			// Tell the user that their account was made
			AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
			alertDialog.setTitle("Account Created");
			alertDialog.setMessage("Your account has been created but it is not enabled.  An administrator needs to enable your account before you can log in.");
			alertDialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					login(null);
				}
			});
			alertDialog.show();
		} else {
			if (accountStatus.getErrorIndices().isEmpty()) {
				getServerEditText().setError("Unable to make your account at this time");
				getServerEditText().requestFocus();
				getUsernameEditText().requestFocus();
			} else {
				int errorMessageIndex = 0;
				for (Integer errorIndex : accountStatus.getErrorIndices()) {
					String message = "Error";
					if (errorMessageIndex < accountStatus.getErrorMessages().size()) {
						message = accountStatus.getErrorMessages().get(errorMessageIndex++);
					}
					if (errorIndex == 0) {
						getFirstNameEditText().setError(message);
						getFirstNameEditText().requestFocus();
					} else if (errorIndex == 1) {
						getLastNameEditText().setError(message);
						getLastNameEditText().requestFocus();
					} else if (errorIndex == 2) {
						getUsernameEditText().setError(message);
						getUsernameEditText().requestFocus();
					} else if (errorIndex == 3) {
						getEmailEditText().setError(message);
						getEmailEditText().requestFocus();
					} else if (errorIndex == 4) {
						getPasswordEditText().setError(message);
						getPasswordEditText().requestFocus();
					} else if (errorIndex == 5) {
						getServerEditText().setError(message);
						getServerEditText().requestFocus();
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
}
