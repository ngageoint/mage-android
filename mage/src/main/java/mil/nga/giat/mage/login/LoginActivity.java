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
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
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

	public static final String EXTRA_PICK_DEFAULT_EVENT = "PICK_DEFAULT_EVENT";

	private static final String LOG_NAME = LoginActivity.class.getName();

	private EditText mUsernameEditText;
	private EditText mPasswordEditText;
	private EditText mServerEditText;
	private Button mLoginButton;

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

		if (getIntent().getBooleanExtra("LOGOUT", false)) {
			((MAGE) getApplication()).onLogout(true);
		}

		// IMPORTANT: load the configuration from preferences files and server
		PreferenceHelper preferenceHelper = PreferenceHelper.getInstance(getApplicationContext());
		preferenceHelper.initialize(false, new Class<?>[]{mil.nga.giat.mage.sdk.R.xml.class, R.xml.class});

		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

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

		((TextView) findViewById(R.id.login_version)).setText("Version: " + sharedPreferences.getString(getString(R.string.buildVersionKey), "NA"));
		findViewById(R.id.login_lock).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onLockToggle(v);
			}
		});

		mUsernameEditText = (EditText) findViewById(R.id.login_username);
		mPasswordEditText = (EditText) findViewById(R.id.login_password);
		mPasswordEditText.setTypeface(Typeface.DEFAULT);
		mServerEditText = (EditText) findViewById(R.id.login_server);

		mLoginButton = (Button) findViewById(R.id.login_login_button);

		String serverURL = sharedPreferences.getString(getString(R.string.serverURLKey), getString(R.string.serverURLDefaultValue));

		// set the default values
		getUsernameEditText().setText(sharedPreferences.getString(getString(R.string.usernameKey), getString(R.string.usernameDefaultValue)));
		getUsernameEditText().setSelection(getUsernameEditText().getText().length());
		getServerEditText().setText(serverURL);
		getServerEditText().setSelection(getServerEditText().getText().length());

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

		ImageView lockImageView = (ImageView) findViewById(R.id.login_lock);
		if (StringUtils.isBlank(serverURL)) {
			onServerUnlock(lockImageView);
		} else {
			if (ConnectivityUtility.isOnline(getApplicationContext())) {
				onServerLock(lockImageView);
			}
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

		InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
		if (getCurrentFocus() != null) {
			inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
		}

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
		final DaoStore daoStore = DaoStore.getInstance(getApplicationContext());
		final AbstractAccountTask loginTask = LoginTaskFactory.getInstance(getApplicationContext()).getLoginTask(this, this.getApplicationContext());
		if (!server.equals(serverURLPref) && !daoStore.isDatabaseEmpty()) {
			new AlertDialog.Builder(this).setTitle("Server URL").setMessage("The server URL has been changed.  If you continue, any previous local data will be deleted.  Do you want to continue?").setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					daoStore.resetDatabase();
					loginTask.execute(credentialsArray);
				}
			}).setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					// show form, and hide spinner
					findViewById(R.id.login_status).setVisibility(View.GONE);
					findViewById(R.id.login_form).setVisibility(View.VISIBLE);
				}
			}).show();
		} else {
			loginTask.execute(credentialsArray);
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

	/**
	 * Fired when user clicks lock
	 *
	 * @param view
	 */
	public void onLockToggle(View view) {
		final ImageView lockImageView = ((ImageView) findViewById(R.id.login_lock));
		if (lockImageView.getTag().toString().equals("lock")) {
			onServerUnlock(lockImageView);
		} else {
			onServerLock(lockImageView);
		}
	}

	private void onServerUnlock(final ImageView lockImageView) {
		if (ConnectivityUtility.isOnline(getApplicationContext())) {
			getServerEditText().setEnabled(true);
			mLoginButton.setEnabled(false);
			lockImageView.setTag("unlock");
			lockImageView.setImageResource(R.drawable.unlock_108);
			showKeyboard();
			getServerEditText().requestFocus();
		} else {
			new AlertDialog.Builder(this)
				.setTitle("No Connectivity")
				.setMessage("Sorry, you cannot change the server URL with no network connectivity.")
				.setPositiveButton(android.R.string.ok, new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				}).show();
		}
	}

	private void onServerLock(final ImageView lockImageView) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		String serverURLPref =  sharedPreferences.getString(getString(R.string.serverURLKey), getString(R.string.serverURLDefaultValue));
		if (StringUtils.isNoneBlank(serverURLPref) && serverURLPref.equals(getServerEditText().getText().toString())) {
			// Server URL was previously set in preferences and did not change.
			// no need to hit the server again
			getServerEditText().setEnabled(false);
			mLoginButton.setEnabled(true);
			lockImageView.setTag("lock");
			lockImageView.setImageResource(R.drawable.lock_108);
			getServerEditText().setError(null);

			return;
		}

		final String url = getServerEditText().getText().toString();
		final View serverProgress = findViewById(R.id.login_server_progress);

		lockImageView.setVisibility(View.GONE);
		serverProgress.setVisibility(View.VISIBLE);

		PreferenceHelper.getInstance(getApplicationContext()).validateServerApi(url, new Predicate<Exception>() {
			@Override
			public boolean apply(Exception e) {
				serverProgress.setVisibility(View.GONE);
				lockImageView.setVisibility(View.VISIBLE);

				if (e == null) {
					getServerEditText().setEnabled(false);
					mLoginButton.setEnabled(true);
					lockImageView.setTag("lock");
					lockImageView.setImageResource(R.drawable.lock_108);
					getServerEditText().setError(null);
					return true;
				} else {
					mLoginButton.setEnabled(false);
					getServerEditText().setEnabled(true);
					showKeyboard();
					getServerEditText().setError(e.getMessage());
					getServerEditText().requestFocus();
					return false;
				}
			}
		});
	}

	private void showKeyboard() {
		InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
		inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0);
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

			// remove the slashes at the end, and store the serverURL
			sp.putString(getApplicationContext().getString(R.string.serverURLKey), getServerEditText().getText().toString().trim().replaceAll("/*$", "")).commit();

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
			// don't store password hash this time
			sp.putString(getApplicationContext().getString(R.string.serverURLKey), getServerEditText().getText().toString()).commit();
			new AlertDialog.Builder(this).setTitle("Registration Sent").setMessage(R.string.device_registered_text).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					findViewById(R.id.login_status).setVisibility(View.GONE);
					findViewById(R.id.login_form).setVisibility(View.VISIBLE);
				}
			}).show();
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
		boolean disclaimerAccepted = sharedPreferences.getBoolean(getString(R.string.disclaimerAccepted), false);

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

		showKeyboard();
		// show form, and hide spinner
		findViewById(R.id.login_status).setVisibility(View.GONE);
		findViewById(R.id.login_form).setVisibility(View.VISIBLE);
	}
}
