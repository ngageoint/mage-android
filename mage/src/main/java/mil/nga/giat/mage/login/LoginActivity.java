package mil.nga.giat.mage.login;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.common.base.Predicate;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

import mil.nga.giat.mage.MAGE;
import mil.nga.giat.mage.R;
import mil.nga.giat.mage.disclaimer.DisclaimerActivity;
import mil.nga.giat.mage.event.EventActivity;
import mil.nga.giat.mage.sdk.connectivity.ConnectivityUtility;
import mil.nga.giat.mage.sdk.datastore.DaoStore;
import mil.nga.giat.mage.sdk.login.AbstractAccountTask;
import mil.nga.giat.mage.sdk.login.AccountDelegate;
import mil.nga.giat.mage.sdk.login.AccountStatus;
import mil.nga.giat.mage.sdk.login.LoginTaskFactory;
import mil.nga.giat.mage.sdk.preferences.PreferenceHelper;
import mil.nga.giat.mage.sdk.utils.PasswordUtility;
import mil.nga.giat.mage.sdk.utils.UserUtility;

/**
 * The login screen
 *
 * @author wiedemanns
 */
public class LoginActivity extends FragmentActivity implements AccountDelegate {

	public static final int EXTRA_OAUTH_RESULT = 1;

	public static final String EXTRA_PICK_DEFAULT_EVENT = "PICK_DEFAULT_EVENT";
	public static final String EXTRA_OAUTH_ERROR = "OAUTH_ERROR";
	public static final String EXTRA_OAUTH_UNREGISTERED_DEVICE = "OAUTH_UNREGISTERED_DEVICE";

	private static final String LOG_NAME = LoginActivity.class.getName();

	private EditText mUsernameEditText;
	private EditText mPasswordEditText;
	private TextView mServerURL;
	private Button mLoginButton;

	public final EditText getUsernameEditText() {
		return mUsernameEditText;
	}

	public final EditText getPasswordEditText() {
		return mPasswordEditText;
	}

	public final TextView getServerUrlText() {
		return mServerURL;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (getIntent().getBooleanExtra("LOGOUT", false)) {
			((MAGE) getApplication()).onLogout(true);
		}

		// IMPORTANT: load the configuration from preferences files and server
		PreferenceHelper preferenceHelper = PreferenceHelper.getInstance(getApplicationContext());
		preferenceHelper.initialize(false, new Class<?>[]{mil.nga.giat.mage.sdk.R.xml.class, R.xml.class});

		final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

		// check if the database needs to be upgraded, and if so log them out
		if (DaoStore.DATABASE_VERSION != sharedPreferences.getInt(getResources().getString(R.string.databaseVersionKey), 0)) {
			((MAGE) getApplication()).onLogout(true);
		}

		sharedPreferences.edit().putInt(getString(R.string.databaseVersionKey), DaoStore.DATABASE_VERSION).commit();

		// check google play services version
		int isGooglePlayServicesAvailable = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getApplicationContext());
		if (isGooglePlayServicesAvailable != ConnectionResult.SUCCESS) {
			if (GooglePlayServicesUtil.isUserRecoverableError(isGooglePlayServicesAvailable)) {
				Dialog dialog = GooglePlayServicesUtil.getErrorDialog(isGooglePlayServicesAvailable, this, 1);
				dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
					@Override
					public void onCancel(DialogInterface dialog) {
						dialog.dismiss();
						finish();
					}
				});
				dialog.show();
			} else {
				new AlertDialog.Builder(this).setTitle("Google Play Services").setMessage("Google Play Services is not installed, or needs to be updated.  Please update Google Play Services before continuing.").setPositiveButton(android.R.string.ok, new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						finish();
					}
				}).show();
			}
		}

		// if token is not expired, then skip the login module
		if (!UserUtility.getInstance(getApplicationContext()).isTokenExpired()) {
			skipLogin();
		}

		// no title bar
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_login);
		hideKeyboardOnClick(findViewById(R.id.login));

		((TextView) findViewById(R.id.login_version)).setText("App Version: " + sharedPreferences.getString(getString(R.string.buildVersionKey), "NA"));

		mUsernameEditText = (EditText) findViewById(R.id.login_username);
		mPasswordEditText = (EditText) findViewById(R.id.login_password);
		mPasswordEditText.setTypeface(Typeface.DEFAULT);
		mServerURL = (TextView) findViewById(R.id.server_url);

		mLoginButton = (Button) findViewById(R.id.local_login_button);

		// set the default values
		getUsernameEditText().setText(sharedPreferences.getString(getString(R.string.usernameKey), getString(R.string.usernameDefaultValue)));
		getUsernameEditText().setSelection(getUsernameEditText().getText().length());

		mPasswordEditText.setOnKeyListener(new View.OnKeyListener() {
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

		final String serverURL = sharedPreferences.getString(getString(R.string.serverURLKey), getString(R.string.serverURLDefaultValue));
		if (StringUtils.isNotEmpty(serverURL)) {
			mServerURL.setText(serverURL);

			PreferenceHelper.getInstance(getApplicationContext()).validateServerApi(serverURL, new Predicate<Exception>() {
				@Override
				public boolean apply(Exception e) {
					if (e == null) {
						mLoginButton.setEnabled(true);
						getServerUrlText().setError(null);
						configureLogin();

						return true;
					} else {
						configureLogin();
						getServerUrlText().setError(e.getMessage());
						getServerUrlText().requestFocus();

						return false;
					}
				}
			});
		}
	}

	public void togglePassword(View v) {
		CheckBox checkbox = (CheckBox) v;
		if (checkbox.isChecked()) {
			mPasswordEditText.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
		} else {
			mPasswordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
		}
		mPasswordEditText.setSelection(mPasswordEditText.getText().length());
		mPasswordEditText.setTypeface(Typeface.DEFAULT);
	}

	private void configureLogin() {
		boolean noServer = StringUtils.isEmpty(mServerURL.getText());
		findViewById(R.id.login_form).setVisibility(noServer ? View.GONE : View.VISIBLE);
		findViewById(R.id.server_configuration).setVisibility(noServer ? View.VISIBLE : View.GONE);

		PreferenceHelper preferenceHelper = PreferenceHelper.getInstance(getApplicationContext());
		if (preferenceHelper.containsLocalAuthentication()) {
			Button localButton = (Button) findViewById(R.id.local_login_button);
			localButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					login(v);
				}
			});
		}

		if (preferenceHelper.containsGoogleAuthentication()) {
			Button googleButton = (Button) findViewById(R.id.google_login_button);
			googleButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					googleLogin();
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

	public void changeServerURL(View view) {
		if (ConnectivityUtility.isOnline(getApplicationContext())) {
			View dialogView = getLayoutInflater().inflate(R.layout.dialog_server, null);
			final EditText serverEditText = (EditText) dialogView.findViewById(R.id.server_url);
			final View progress = dialogView.findViewById(R.id.progress);

			final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
			final String serverURLPreference = sharedPreferences.getString(getString(R.string.serverURLKey), getString(R.string.serverURLDefaultValue));
			serverEditText.setText(serverURLPreference);

			AlertDialog.Builder builder = new AlertDialog.Builder(this)
					.setView(dialogView)
					.setTitle("MAGE Server URL")
					.setPositiveButton(android.R.string.ok, null)
					.setNegativeButton(android.R.string.cancel, null);

			if (StringUtils.isNotEmpty(serverURLPreference)) {
				builder.setMessage("Changing the server URL will delete all previous data.  Do you want to continue?");
			}

			final AlertDialog alertDialog = builder.create();

			alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
				@Override
				public void onShow(DialogInterface dialog) {

					final Button button = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
					button.setOnClickListener(new View.OnClickListener() {

						@Override
						public void onClick(View view) {
							progress.setVisibility(View.VISIBLE);
							button.setEnabled(false);

							String url = serverEditText.getText().toString().trim().replaceAll("(\\w)/*$", "$1");
							if (StringUtils.isNotEmpty(url) && !url.matches("^(H|h)(T|t)(T|t)(P|p)(S|s)?://.*")) {
								url = "https://" + url;
							}

							if (url.equals(serverURLPreference) && getServerUrlText().getError() == null) {
								alertDialog.dismiss();
								return;
							}

							final String serverURL = url;
							PreferenceHelper.getInstance(getApplicationContext()).validateServerApi(serverURL, new Predicate<Exception>() {
								@Override
								public boolean apply(Exception e) {
									if (e == null) {
										mServerURL.setText(serverURL);
										mLoginButton.setEnabled(true);
										serverEditText.setError(null);
										alertDialog.dismiss();

										final DaoStore daoStore = DaoStore.getInstance(getApplicationContext());
										if (!daoStore.isDatabaseEmpty()) {
											daoStore.resetDatabase();
										}

										Editor editor = sharedPreferences.edit();
										editor.putString(getString(R.string.serverURLKey), serverURL).commit();
										configureLogin();

										return true;
									} else {
										progress.setVisibility(View.INVISIBLE);
										button.setEnabled(true);
										serverEditText.setError(e.getMessage());
										getServerUrlText().setError(null);
										return false;
									}
								}
							});

						}
					});
				}
			});

			alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
			alertDialog.show();
		} else {
			new AlertDialog.Builder(this)
					.setTitle("No Connectivity")
					.setMessage("Sorry, you cannot change the server URL with no network connectivity.")
					.setPositiveButton(android.R.string.ok, null).show();
		}
	}

	/**
	 * Fired when user clicks login
	 *
	 * @param view
	 */
	public void login(View view) {

		InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
		if (getCurrentFocus() != null) {
			inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
		}

		// reset errors
		getUsernameEditText().setError(null);
		getPasswordEditText().setError(null);
		mServerURL.setError(null);

		String username = getUsernameEditText().getText().toString();
		String password = getPasswordEditText().getText().toString();
		String server = mServerURL.getText().toString();

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
		credentials.add(Boolean.FALSE.toString());
		final String[] credentialsArray = credentials.toArray(new String[credentials.size()]);

		// show spinner, and hide form
		findViewById(R.id.login_form).setVisibility(View.GONE);
		findViewById(R.id.login_status).setVisibility(View.VISIBLE);

		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
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
		intent.putExtra(OAuthActivity.EXTRA_SERVER_URL, mServerURL.getText());
		intent.putExtra(OAuthActivity.EXTRA_OAUTH_URL, mServerURL.getText() + "/auth/google/signin");
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

	/**
	 * Fired when user clicks signup
	 *
	 * @param view
	 */
	public void signup(View view) {
		Intent intent = new Intent(getApplicationContext(), SignupActivity.class);
		startActivity(intent);
	}

	@Override
	public void finishAccount(AccountStatus accountStatus) {
		if (accountStatus.getStatus().equals(AccountStatus.Status.SUCCESSFUL_LOGIN) || accountStatus.getStatus().equals(AccountStatus.Status.DISCONNECTED_LOGIN)) {
			Editor sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
			sp.putString(getApplicationContext().getString(R.string.usernameKey), getUsernameEditText().getText().toString()).commit();
			try {
				String hashedPassword = PasswordUtility.getSaltedHash(getPasswordEditText().getText().toString());
				sp.putString(getApplicationContext().getString(R.string.passwordHashKey), hashedPassword).commit();
			} catch (Exception e) {
				Log.e(LOG_NAME, "Could not hash password", e);
			}

			PreferenceHelper.getInstance(getApplicationContext()).logKeyValuePairs();

			if (accountStatus.getStatus().equals(AccountStatus.Status.DISCONNECTED_LOGIN)) {
				new AlertDialog.Builder(this).setTitle("Disconnected Login").setMessage("You are logging into MAGE in disconnected mode.  You must re-establish a connection in order to push and pull information to and from your server.").setPositiveButton(android.R.string.ok, new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						startNextActivityAndFinish();
					}
				}).show();
			} else {
				startNextActivityAndFinish();
			}
		} else if (accountStatus.getStatus().equals(AccountStatus.Status.SUCCESSFUL_REGISTRATION)) {
			Editor sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
			sp.putString(getApplicationContext().getString(R.string.usernameKey), getUsernameEditText().getText().toString()).commit();
			showUnregisteredDeviceDialog();
		} else {
			if (accountStatus.getErrorIndices().isEmpty()) {
				getUsernameEditText().setError(null);
				getPasswordEditText().setError(null);
				new AlertDialog.Builder(this).setTitle("Incorrect Credentials").setMessage("The username or password you entered was incorrect.").setPositiveButton(android.R.string.ok, new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				}).show();
				getPasswordEditText().requestFocus();
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
						mServerURL.setError(message);
						mServerURL.requestFocus();
					}
				}
			}
			// show form, and hide spinner
			findViewById(R.id.login_status).setVisibility(View.GONE);
			findViewById(R.id.login_form).setVisibility(View.VISIBLE);
		}
	}

	public void startNextActivityAndFinish() {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		boolean showDisclaimer = sharedPreferences.getBoolean(getString(R.string.serverDisclaimerShow), false);

		Intent intent = showDisclaimer ?
				new Intent(getApplicationContext(), DisclaimerActivity.class) :
				new Intent(getApplicationContext(), EventActivity.class);

		startActivity(intent);
		finish();
	}

	public void skipLogin() {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		boolean disclaimerAccepted = sharedPreferences.getBoolean(getString(R.string.disclaimerAcceptedKey), getResources().getBoolean(R.bool.disclaimerAcceptedDefaultValue));

		Intent intent = disclaimerAccepted ?
				new Intent(getApplicationContext(), EventActivity.class) :
				new Intent(getApplicationContext(), DisclaimerActivity.class);

		intent.putExtra(EventActivity.EXTRA_CHOOSE_CURRENT_EVENT, true);
		startActivity(intent);
		finish();
	}

	@Override
	protected void onResume() {
		super.onResume();

		if (getIntent().getBooleanExtra("LOGOUT", false)) {
			((MAGE) getApplication()).onLogout(true);
		}

		configureLogin();

		// show form, and hide spinner
		findViewById(R.id.login_status).setVisibility(View.GONE);
	}

	private void showUnregisteredDeviceDialog() {
		new AlertDialog.Builder(this)
				.setTitle("Registration Sent")
				.setMessage(R.string.device_registered_text)
				.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						findViewById(R.id.login_status).setVisibility(View.GONE);
						findViewById(R.id.login_form).setVisibility(View.VISIBLE);
					}
				}).show();
	}

	private void showOAuthErrorDialog() {
		new AlertDialog.Builder(this)
				.setTitle("Login failed")
				.setMessage("Could not login w/ account. Either your account does not exist or it has not been approved.")
				.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						findViewById(R.id.login_status).setVisibility(View.GONE);
						findViewById(R.id.login_form).setVisibility(View.VISIBLE);
					}
				}).show();
	}
}
