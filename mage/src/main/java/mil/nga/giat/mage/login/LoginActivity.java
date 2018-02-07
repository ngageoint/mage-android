package mil.nga.giat.mage.login;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

import mil.nga.giat.mage.LandingActivity;
import mil.nga.giat.mage.MAGE;
import mil.nga.giat.mage.R;
import mil.nga.giat.mage.cache.CacheUtils;
import mil.nga.giat.mage.disclaimer.DisclaimerActivity;
import mil.nga.giat.mage.event.EventActivity;
import mil.nga.giat.mage.sdk.datastore.DaoStore;
import mil.nga.giat.mage.sdk.datastore.user.Event;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.datastore.user.UserHelper;
import mil.nga.giat.mage.sdk.exceptions.UserException;
import mil.nga.giat.mage.sdk.login.AccountStatus;
import mil.nga.giat.mage.sdk.preferences.PreferenceHelper;
import mil.nga.giat.mage.sdk.utils.MediaUtility;
import mil.nga.giat.mage.sdk.utils.PasswordUtility;
import mil.nga.giat.mage.sdk.utils.UserUtility;

/**
 * The login screen
 *
 * @author wiedemanns
 */
public class LoginActivity extends AppCompatActivity implements LoginFragment.LoginListener {

	public static final int EXTRA_OAUTH_RESULT = 1;

	public static final String EXTRA_PICK_DEFAULT_EVENT = "PICK_DEFAULT_EVENT";
	public static final String EXTRA_OAUTH_ERROR = "OAUTH_ERROR";
	public static final String EXTRA_OAUTH_UNREGISTERED_DEVICE = "OAUTH_UNREGISTERED_DEVICE";
	public static final String EXTRA_CONTINUE_SESSION = "CONTINUE_SESSION";
	public static final String EXTRA_CONTINUE_SESSION_WHILE_USING = "CONTINUE_SESSION_WHILE_USING";

	private static final String LOG_NAME = LoginActivity.class.getName();

	private EditText mUsernameEditText;
	private TextInputLayout mUsernameLayout;

	private EditText mPasswordEditText;
	private TextInputLayout mPasswordLayout;

	private TextView mServerURL;

	private String mOpenFilePath;

	private String currentUsername;
	private boolean mContinueSession;

	private static final String LOGIN_FRAGMENT_TAG = "LOGIN_FRAGMENT";
	private LoginFragment loginFragment;

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

		Intent intent = getIntent();

		mContinueSession = getIntent().getBooleanExtra(EXTRA_CONTINUE_SESSION, false);

		boolean continueSessionWhileUsing = getIntent().getBooleanExtra(EXTRA_CONTINUE_SESSION_WHILE_USING, false);
		intent.removeExtra(EXTRA_CONTINUE_SESSION_WHILE_USING);
		if (continueSessionWhileUsing && savedInstanceState == null) {
			showSessionExpiredDialog();
		}

		if (intent.getBooleanExtra("LOGOUT", false)) {
			((MAGE) getApplication()).onLogout(true, null);
		}

		// IMPORTANT: load the configuration from preferences files and server
		PreferenceHelper preferenceHelper = PreferenceHelper.getInstance(getApplicationContext());
		preferenceHelper.initialize(false, new Class<?>[]{mil.nga.giat.mage.sdk.R.xml.class, R.xml.class});

		final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

		// check if the database needs to be upgraded, and if so log them out
		if (DaoStore.DATABASE_VERSION != sharedPreferences.getInt(getResources().getString(R.string.databaseVersionKey), 0)) {
			((MAGE) getApplication()).onLogout(true, null);
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

		// Handle when MAGE was launched with a Uri (such as a local or remote cache file)
		Uri uri = intent.getData();
		if(uri == null){
			Bundle bundle = intent.getExtras();
			if(bundle != null){
				Object objectUri = bundle.get(Intent.EXTRA_STREAM);
				if(objectUri != null){
					uri = (Uri)objectUri;
				}
			}
		}
		if (uri != null) {
			handleUri(uri);
		}

		// if token is not expired, then skip the login module
		if (!UserUtility.getInstance(getApplicationContext()).isTokenExpired()) {
			skipLogin();
		}

		// no title bar
		setContentView(R.layout.activity_login);
		hideKeyboardOnClick(findViewById(R.id.login));

		TextView appName = (TextView) findViewById(R.id.mage);
		appName.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/GondolaMage-Regular.otf"));

		((TextView) findViewById(R.id.login_version)).setText("App Version: " + sharedPreferences.getString(getString(R.string.buildVersionKey), "NA"));

		mUsernameEditText = (EditText) findViewById(R.id.login_username);
		mUsernameLayout = (TextInputLayout) findViewById(R.id.username_layout);

		mPasswordEditText = (EditText) findViewById(R.id.login_password);
		mPasswordLayout = (TextInputLayout) findViewById(R.id.password_layout);

		mPasswordEditText.setTypeface(Typeface.DEFAULT);
		mServerURL = (TextView) findViewById(R.id.server_url);

		String serverURL = sharedPreferences.getString(getString(R.string.serverURLKey), getString(R.string.serverURLDefaultValue));
		if (StringUtils.isEmpty(serverURL)) {
			changeServerURL();
			return;
		}

		findViewById(R.id.server_url).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				changeServerURL();
			}
		});

		// set the default values
		getUsernameEditText().setText(sharedPreferences.getString(getString(R.string.usernameKey), getString(R.string.usernameDefaultValue)));
		getUsernameEditText().setSelection(getUsernameEditText().getText().length());

		mUsernameEditText.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {

			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {

			}

			@Override
			public void afterTextChanged(Editable s) {
				if (StringUtils.isNoneBlank(s)) {
					mUsernameLayout.setError(null);
				}
			}
		});

		mPasswordEditText.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {

			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {

			}

			@Override
			public void afterTextChanged(Editable s) {
				if (StringUtils.isNoneBlank(s)) {
					mPasswordLayout.setError(null);
				}
			}
		});

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

		FragmentManager fragmentManager = getSupportFragmentManager();
		loginFragment = (LoginFragment) fragmentManager.findFragmentByTag(LOGIN_FRAGMENT_TAG);

		// If the Fragment is non-null, then it is being retained over a configuration change.
		if (loginFragment == null) {
			loginFragment = new LoginFragment();
			fragmentManager.beginTransaction().add(loginFragment, LOGIN_FRAGMENT_TAG).commit();
		}

		mServerURL.setText(serverURL);

		// Setup login based on last api pull
		configureLogin();

		findViewById(R.id.login_status).setVisibility(loginFragment.isAuthenticating() ? View.VISIBLE : View.GONE);
		findViewById(R.id.login_form).setVisibility(loginFragment.isAuthenticating() ? View.GONE : View.VISIBLE);
	}

	@Override
	public void onBackPressed() {
		if (mContinueSession) {
			// In this case the activity stack was preserved. Don't allow the user to go back to an activity without logging in.
			// Since this is the application entry point, assume back means go home.
			Intent intent = new Intent(Intent.ACTION_MAIN);
			intent.addCategory(Intent.CATEGORY_HOME);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
			return;
		}

		super.onBackPressed();
	}

	private void configureLogin() {
		PreferenceHelper preferenceHelper = PreferenceHelper.getInstance(getApplicationContext());

		boolean localAuthentication = preferenceHelper.containsLocalAuthentication();
		boolean googleAuthentication = preferenceHelper.containsGoogleAuthentication();

		findViewById(R.id.or).setVisibility(localAuthentication && googleAuthentication ? View.VISIBLE : View.GONE);
		findViewById(R.id.sign_up).setVisibility(localAuthentication || googleAuthentication ? View.VISIBLE : View.GONE);

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

	public void changeServerURL() {
		Intent intent = new Intent(this, ServerUrlActivity.class);
		startActivity(intent);
		finish();
	}

	/**
	 * Handle the Uri used to launch MAGE
	 * @param uri
	 */
	private void handleUri(Uri uri) {

		// Attempt to get a local file path
		String openPath = MediaUtility.getPath(this, uri);

		// If not a local or temporary file path, copy the file to cache
		// Cannot pass this to another activity to handle as the URI might
		// become invalid between now and then.  Copy it now
		if (openPath == null || MediaUtility.isTemporaryPath(openPath)) {
			CacheUtils.copyToCache(this, uri, openPath);
		} else {
			// Else, store the path to pass to further intents
			mOpenFilePath = openPath;
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

		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		currentUsername = sharedPreferences.getString(getApplicationContext().getString(mil.nga.giat.mage.sdk.R.string.usernameKey), null);

		// reset errors
		mUsernameLayout.setError(null);
		mPasswordLayout.setError(null);
		mServerURL.setError(null);

		String username = getUsernameEditText().getText().toString();
		String password = getPasswordEditText().getText().toString();
		String server = mServerURL.getText().toString();

		// are the inputs valid?
		if (TextUtils.isEmpty(username)) {
			mUsernameLayout.setError("Username can not be blank");
			return;
		}

		if (TextUtils.isEmpty(password)) {
			mPasswordLayout.setError("Password can not be blank");
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

		loginFragment.authenticate(credentialsArray);
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
		finish();
	}

	@Override
	public void onApi(boolean valid) {
		if (!loginFragment.isAuthenticating()) {
			configureLogin();
		}
	}

	@Override
	public void onAuthentication(AccountStatus accountStatus) {
		if (accountStatus.getStatus().equals(AccountStatus.Status.SUCCESSFUL_LOGIN) || accountStatus.getStatus().equals(AccountStatus.Status.DISCONNECTED_LOGIN)) {
			SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
			Editor editor = sharedPreferences.edit();
			editor.putString(getApplicationContext().getString(R.string.usernameKey), getUsernameEditText().getText().toString()).commit();
			try {
				String hashedPassword = PasswordUtility.getSaltedHash(getPasswordEditText().getText().toString());
				editor.putString(getApplicationContext().getString(R.string.passwordHashKey), hashedPassword).commit();
			} catch (Exception e) {
				Log.e(LOG_NAME, "Could not hash password", e);
			}

			PreferenceHelper.getInstance(getApplicationContext()).logKeyValuePairs();

			final boolean sameUser = sharedPreferences.getString(getApplicationContext().getString(mil.nga.giat.mage.sdk.R.string.usernameKey), "").equals(currentUsername);
			final boolean preserveActivityStack = sameUser && mContinueSession;

			if (accountStatus.getStatus().equals(AccountStatus.Status.DISCONNECTED_LOGIN)) {
				new AlertDialog.Builder(this)
						.setTitle("Disconnected Login")
						.setMessage("You are logging into MAGE in disconnected mode.  You must re-establish a connection in order to push and pull information to and from your server.")
						.setPositiveButton(android.R.string.ok, new OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.dismiss();
								startNextActivityAndFinish(preserveActivityStack);
							}
						})
						.setCancelable(false)
						.show();
			} else {
				startNextActivityAndFinish(preserveActivityStack);
			}
		} else if (accountStatus.getStatus().equals(AccountStatus.Status.SUCCESSFUL_REGISTRATION)) {
			Editor sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
			sp.putString(getApplicationContext().getString(R.string.usernameKey), getUsernameEditText().getText().toString()).commit();
			showUnregisteredDeviceDialog();
		} else {
			if (accountStatus.getStatus().equals(AccountStatus.Status.INVALID_SERVER)) {
				new AlertDialog.Builder(this)
						.setTitle("Application Compatibility Error")
						.setMessage("This app is not compatible with this server. Please update your application or talk to your MAGE administrator.")
						.setPositiveButton(android.R.string.ok, new OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.dismiss();
							}
						}).show();
			} else if (accountStatus.getErrorIndices().isEmpty()) {
				getUsernameEditText().setError(null);
				getPasswordEditText().setError(null);
				new AlertDialog.Builder(this)
						.setTitle("Incorrect Credentials")
						.setMessage("The username or password you entered was incorrect.")
						.setPositiveButton(android.R.string.ok, new OnClickListener() {
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
						new AlertDialog.Builder(this)
							.setTitle("Login Failed")
							.setMessage(message)
							.setPositiveButton(android.R.string.ok, null)
							.show();
					}
				}
			}
			// show form, and hide spinner
			findViewById(R.id.login_status).setVisibility(View.GONE);
			findViewById(R.id.login_form).setVisibility(View.VISIBLE);
		}
	}

	public void startNextActivityAndFinish(boolean preserveActivityStack) {

		if (preserveActivityStack) {
			// We are going to return user to the app where they last left off,
			// make sure to start up MAGE services
			((MAGE) getApplication()).onLogin();

			// TODO look at refreshing the event here...
		} else {
			SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
			boolean showDisclaimer = sharedPreferences.getBoolean(getString(R.string.serverDisclaimerShow), false);

			Intent intent = showDisclaimer ?
					new Intent(getApplicationContext(), DisclaimerActivity.class) :
					new Intent(getApplicationContext(), EventActivity.class);

			// If launched with a local file path, save as an extra
			if (mOpenFilePath != null) {
				intent.putExtra(LandingActivity.EXTRA_OPEN_FILE_PATH, mOpenFilePath);
			}

			startActivity(intent);
		}

		finish();
	}

	public void skipLogin() {
		Intent intent;

		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		boolean disclaimerAccepted = sharedPreferences.getBoolean(getString(R.string.disclaimerAcceptedKey), false);
		if (disclaimerAccepted) {
			Event event = null;
			try {
				User user = UserHelper.getInstance(getApplicationContext()).readCurrentUser();
				event = user.getCurrentEvent();
			} catch (UserException e) {
				e.printStackTrace();
			}

			intent = event == null ?
				new Intent(getApplicationContext(), EventActivity.class) :
				new Intent(getApplicationContext(), LandingActivity.class);
		} else {
			intent = new Intent(getApplicationContext(), DisclaimerActivity.class);
		}

		// If launched with a local file path, save as an extra
		if (mOpenFilePath != null) {
			intent.putExtra(LandingActivity.EXTRA_OPEN_FILE_PATH, mOpenFilePath);
		}

		startActivity(intent);
		finish();
	}

	@Override
	protected void onResume() {
		super.onResume();

		if (getIntent().getBooleanExtra("LOGOUT", false)) {
			((MAGE) getApplication()).onLogout(true, null);
		}
	}

	private void showUnregisteredDeviceDialog() {
		findViewById(R.id.login_status).setVisibility(View.GONE);
		findViewById(R.id.login_form).setVisibility(View.VISIBLE);

		new AlertDialog.Builder(this)
				.setTitle("Registration Sent")
				.setMessage(R.string.device_registered_text)
				.setPositiveButton(android.R.string.ok, null)
				.show();
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

	private void showSessionExpiredDialog() {
		AlertDialog dialog = new AlertDialog.Builder(this)
				.setTitle("Session Expired")
				.setCancelable(false)
				.setMessage("We apologize, but it looks like your MAGE session has expired.  Please login and we will take you back to what you were doing.")
				.setPositiveButton(android.R.string.ok, null).create();

		dialog.setCanceledOnTouchOutside(false);

		dialog.show();
	}}
