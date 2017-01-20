package mil.nga.giat.mage.login;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.Html;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

import mil.nga.giat.mage.MAGE;
import mil.nga.giat.mage.R;
import mil.nga.giat.mage.disclaimer.DisclaimerActivity;
import mil.nga.giat.mage.sdk.login.AbstractAccountTask;
import mil.nga.giat.mage.sdk.login.AccountDelegate;
import mil.nga.giat.mage.sdk.login.AccountStatus;
import mil.nga.giat.mage.sdk.login.LoginTaskFactory;
import mil.nga.giat.mage.sdk.preferences.PreferenceHelper;
import mil.nga.giat.mage.sdk.utils.PasswordUtility;
import mil.nga.giat.mage.sdk.utils.UserUtility;

/**
 * The login screen, when a users token expires.  This will be a non-dismissable activity until the
 * user successfully authenticates.  Upon successfull authentication the activity will be dismissed
 * leaving the user where they left off before token expiration.
 */
public class TokenExpiredActivity extends AppCompatActivity implements AccountDelegate {

	private static final int EXTRA_OAUTH_RESULT = 100;

	private static final String EXTRA_OAUTH_UNREGISTERED_DEVICE = "OAUTH_UNREGISTERED_DEVICE";

	private static final String LOG_NAME = TokenExpiredActivity.class.getName();

	private EditText usernameEditText;
	private EditText passwordEditText;
	private TextView passwordToggle;
	private TextView serverUrl;

	private String currentUsername;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_token_expiration);

		final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

		String displayName = sharedPreferences.getString(getApplicationContext().getString(mil.nga.giat.mage.sdk.R.string.displayNameKey), "");
		String message = getResources().getString(R.string.reauthentication_message, displayName);
		((TextView) findViewById(R.id.message_text_view)).setText(Html.fromHtml(message));

		usernameEditText = (EditText) findViewById(R.id.login_username);
		passwordEditText = (EditText) findViewById(R.id.login_password);
		passwordEditText.setTypeface(Typeface.DEFAULT);
		serverUrl = (TextView) findViewById(R.id.server_url);

		passwordToggle = (TextView) findViewById(R.id.toggle_password);
		passwordToggle.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				togglePassword(v);
			}
		});

		// set the default values
		usernameEditText.setText(sharedPreferences.getString(getString(R.string.usernameKey), getString(R.string.usernameDefaultValue)));
		usernameEditText.setSelection(usernameEditText.getText().length());

		String serverURL = sharedPreferences.getString(getString(R.string.serverURLKey), getString(R.string.serverURLDefaultValue));
		serverUrl.setText(serverURL);

		passwordEditText.setOnKeyListener(new View.OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_ENTER) {
					login(v);
					return true;
				} else {
					return false;
				}
			}
		});

		passwordEditText.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {

			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {

			}

			@Override
			public void afterTextChanged(Editable s) {
				passwordToggle.setVisibility(s.length() == 0 ? View.GONE : View.VISIBLE);
			}
		});

		configureLogin();
	}

	@Override
	public void onBackPressed() {
		Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_HOME);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
	}

	public void togglePassword(View v) {
		TextView textView = (TextView) v;

		if (textView.getText().toString().equalsIgnoreCase("SHOW")) {
			textView.setText("HIDE");
			passwordEditText.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
		} else {
			textView.setText("SHOW");
			passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
		}
		passwordEditText.setSelection(passwordEditText.getText().length());
		passwordEditText.setTypeface(Typeface.DEFAULT);
	}

	private void configureLogin() {
		PreferenceHelper preferenceHelper = PreferenceHelper.getInstance(getApplicationContext());

		boolean noServer = StringUtils.isEmpty(serverUrl.getText());
		boolean localAuthentication = preferenceHelper.containsLocalAuthentication();
		boolean googleAuthentication = preferenceHelper.containsGoogleAuthentication();

		findViewById(R.id.login_form).setVisibility(noServer ? View.GONE : View.VISIBLE);
		findViewById(R.id.or).setVisibility(localAuthentication && googleAuthentication ? View.VISIBLE : View.GONE);

		if (localAuthentication) {
			Button localButton = (Button) findViewById(R.id.local_login_button);
			localButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					login(v);
				}
			});
			findViewById(R.id.local_auth).setVisibility(View.VISIBLE);
		} else {
			findViewById(R.id.local_auth).setVisibility(View.GONE);
		}

		if (googleAuthentication) {
			Button googleButton = (Button) findViewById(R.id.google_login_button);
			googleButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					googleLogin();
				}
			});
			findViewById(R.id.third_party_auth).setVisibility(View.VISIBLE);
		} else {
			findViewById(R.id.third_party_auth).setVisibility(View.GONE);
		}
	}

	/**
	 * Fired when user clicks login
	 *
	 * @param view
	 */
	public void login(View view) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		currentUsername = sharedPreferences.getString(getApplicationContext().getString(mil.nga.giat.mage.sdk.R.string.usernameKey), null);

		InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
		if (getCurrentFocus() != null) {
			inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
		}

		// reset errors
		usernameEditText.setError(null);
		passwordEditText.setError(null);
		serverUrl.setError(null);

		String username = usernameEditText.getText().toString();
		String password = passwordEditText.getText().toString();
		String server = serverUrl.getText().toString();

		// are the inputs valid?
		if (TextUtils.isEmpty(username)) {
			usernameEditText.setError("Username can not be blank");
			usernameEditText.requestFocus();
			return;
		}

		if (TextUtils.isEmpty(password)) {
			passwordEditText.setError("Password can not be blank");
			passwordEditText.requestFocus();
			return;
		}

		List<String> credentials = new ArrayList<String>();
		credentials.add(username);
		credentials.add(password);
		credentials.add(server);
		credentials.add(Boolean.FALSE.toString());
		final String[] credentialsArray = credentials.toArray(new String[credentials.size()]);

		// show spinner, and hide form
		findViewById(R.id.login_form).setVisibility(View.GONE);
		findViewById(R.id.login_status).setVisibility(View.VISIBLE);

		String serverURLPref =  sharedPreferences.getString(getString(R.string.serverURLKey), getString(R.string.serverURLDefaultValue));

		// if the username is different, then clear the token information
		String oldUsername = sharedPreferences.getString(getString(R.string.usernameKey), null);
		if (StringUtils.isNotEmpty(oldUsername) && (!username.equals(oldUsername) || !server.equals(serverURLPref))) {
			PreferenceHelper preferenceHelper = PreferenceHelper.getInstance(getApplicationContext());
			preferenceHelper.initialize(true, new Class<?>[]{mil.nga.giat.mage.sdk.R.xml.class, R.xml.class});
			UserUtility.getInstance(getApplicationContext()).clearTokenInformation();
		}

		// if the serverURL is different that before, clear out the database
		AbstractAccountTask loginTask = LoginTaskFactory.getInstance(getApplicationContext()).getLoginTask(this, this.getApplicationContext());
		loginTask.execute(credentialsArray);
	}

	private void googleLogin() {
		Intent intent = new Intent(getApplicationContext(), OAuthActivity.class);
		intent.putExtra(OAuthActivity.EXTRA_SERVER_URL, serverUrl.getText());
		intent.putExtra(OAuthActivity.EXTRA_OAUTH_URL, serverUrl.getText() + "/auth/google/signin");
		intent.putExtra(OAuthActivity.EXTRA_OAUTH_TYPE, OAuthActivity.OAuthType.SIGNIN);
		startActivityForResult(intent, EXTRA_OAUTH_RESULT);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (requestCode == EXTRA_OAUTH_RESULT && resultCode == RESULT_OK) {
			if (intent.getBooleanExtra(EXTRA_OAUTH_UNREGISTERED_DEVICE, false)) {
				showUnregisteredDeviceDialog();
			} else {
				showOAuthErrorDialog();
			}
		}
	}

	@Override
	public void finishAccount(AccountStatus accountStatus) {
		if (accountStatus.getStatus().equals(AccountStatus.Status.SUCCESSFUL_LOGIN) || accountStatus.getStatus().equals(AccountStatus.Status.DISCONNECTED_LOGIN)) {
			SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
			Editor editor = sharedPreferences.edit();
			editor.putString(getApplicationContext().getString(R.string.usernameKey), usernameEditText.getText().toString()).commit();
			try {
				String hashedPassword = PasswordUtility.getSaltedHash(passwordEditText.getText().toString());
				editor.putString(getApplicationContext().getString(R.string.passwordHashKey), hashedPassword).commit();
			} catch (Exception e) {
				Log.e(LOG_NAME, "Could not hash password", e);
			}

			PreferenceHelper.getInstance(getApplicationContext()).logKeyValuePairs();

			final boolean newUser = currentUsername != sharedPreferences.getString(getApplicationContext().getString(mil.nga.giat.mage.sdk.R.string.usernameKey), null);
			if (accountStatus.getStatus().equals(AccountStatus.Status.DISCONNECTED_LOGIN)) {
				new AlertDialog.Builder(this)
						.setTitle("Disconnected Login")
						.setMessage("You are logging into MAGE in disconnected mode.  You must re-establish a connection in order to push and pull information to and from your server.")
						.setPositiveButton(android.R.string.ok, new OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.dismiss();
								startNextActivityAndFinish(newUser);
							}
						}).show();
			} else {
				startNextActivityAndFinish(newUser);
			}
		} else if (accountStatus.getStatus().equals(AccountStatus.Status.SUCCESSFUL_REGISTRATION)) {
			Editor sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
			sp.putString(getApplicationContext().getString(R.string.usernameKey), usernameEditText.getText().toString()).commit();
			showUnregisteredDeviceDialog();
		} else {
			if (accountStatus.getErrorIndices().isEmpty()) {
				usernameEditText.setError(null);
				passwordEditText.setError(null);
				new AlertDialog.Builder(this)
						.setTitle("Incorrect Credentials")
						.setMessage("The username or password you entered was incorrect.")
						.setPositiveButton(android.R.string.ok, new OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.dismiss();
							}
						}).show();
				passwordEditText.requestFocus();
			} else {
				int errorMessageIndex = 0;
				for (Integer errorIndex : accountStatus.getErrorIndices()) {
					String message = "Error";
					if (errorMessageIndex < accountStatus.getErrorMessages().size()) {
						message = accountStatus.getErrorMessages().get(errorMessageIndex++);
					}
					if (errorIndex == 0) {
						usernameEditText.setError(message);
						usernameEditText.requestFocus();
					} else if (errorIndex == 1) {
						passwordEditText.setError(message);
						passwordEditText.requestFocus();
					} else if (errorIndex == 2) {
						serverUrl.setError(message);
						serverUrl.requestFocus();
					}
				}
			}
			// show form, and hide spinner
			findViewById(R.id.login_status).setVisibility(View.GONE);
			findViewById(R.id.login_form).setVisibility(View.VISIBLE);
		}
	}

	public void startNextActivityAndFinish(boolean newUser) {
		if (newUser) {
			Intent intent = new Intent(getApplicationContext(), DisclaimerActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
		} else {
			// We are going to return user to the app where they last left off,
			// make sure to start up MAGE services
			((MAGE) getApplication()).onLogin();
		}

		finish();
	}

	public void returnToLogin(View view) {
		Intent intent = new Intent(this, LoginActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		startActivity(intent);
		finish();
	}

	private void showUnregisteredDeviceDialog() {
		new AlertDialog.Builder(this)
				.setTitle("Registration Sent")
				.setMessage(R.string.device_registered_text)
				.setPositiveButton(android.R.string.ok, new OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						returnToLogin(null);
					}
				}).show();
	}

	private void showOAuthErrorDialog() {
		new AlertDialog.Builder(this)
				.setTitle("Login failed")
				.setMessage("Could not login w/ account. Either your account does not exist or it has not been approved.")
				.setPositiveButton(android.R.string.ok, new OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						findViewById(R.id.login_status).setVisibility(View.GONE);
						findViewById(R.id.login_form).setVisibility(View.VISIBLE);
					}
				}).show();
	}
}
