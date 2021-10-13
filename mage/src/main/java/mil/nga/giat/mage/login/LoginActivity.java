package mil.nga.giat.mage.login;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.work.WorkManager;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import mil.nga.giat.mage.LandingActivity;
import mil.nga.giat.mage.MageApplication;
import mil.nga.giat.mage.R;
import mil.nga.giat.mage._server5.login.SignupActivity_server5;
import mil.nga.giat.mage.cache.CacheUtils;
import mil.nga.giat.mage.disclaimer.DisclaimerActivity;
import mil.nga.giat.mage.event.EventsActivity;
import mil.nga.giat.mage.login.LoginViewModel.Authentication;
import mil.nga.giat.mage.login.idp.IdpLoginFragment;
import mil.nga.giat.mage.login.ldap.LdapLoginFragment;
import mil.nga.giat.mage.login.mage.MageLoginFragment;
import mil.nga.giat.mage.sdk.Compatibility;
import mil.nga.giat.mage.sdk.datastore.DaoStore;
import mil.nga.giat.mage.sdk.datastore.user.Event;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.datastore.user.UserHelper;
import mil.nga.giat.mage.sdk.exceptions.UserException;
import mil.nga.giat.mage.sdk.login.AuthenticationStatus;
import mil.nga.giat.mage.sdk.login.AuthorizationStatus;
import mil.nga.giat.mage.sdk.preferences.PreferenceHelper;
import mil.nga.giat.mage.sdk.utils.MediaUtility;
import mil.nga.giat.mage.sdk.utils.UserUtility;

@AndroidEntryPoint
public class LoginActivity extends AppCompatActivity {

   public static final String EXTRA_CONTINUE_SESSION = "CONTINUE_SESSION";
   public static final String EXTRA_CONTINUE_SESSION_WHILE_USING = "CONTINUE_SESSION_WHILE_USING";

   @Inject
   protected MageApplication application;

   @Inject
   protected SharedPreferences preferences;

   private LoginViewModel viewModel;

   private TextView mServerURL;

   private String mOpenFilePath;

   private boolean mContinueSession;

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
         application.onLogout(true, null);
      }

      // IMPORTANT: load the configuration from preferences files and server
      PreferenceHelper preferenceHelper = PreferenceHelper.getInstance(getApplicationContext());
      // TODO make sure sdk config is loaded
//      preferenceHelper.initialize(false, mil.nga.giat.mage.sdk.R.xml.class, R.xml.class);
      preferenceHelper.initialize(false, R.xml.class);

      // check if the database needs to be upgraded, and if so log them out
      if (DaoStore.DATABASE_VERSION != preferences.getInt(getResources().getString(R.string.databaseVersionKey), 0)) {
         application.onLogout(true, null);
      }

      preferences.edit().putInt(getString(R.string.databaseVersionKey), DaoStore.DATABASE_VERSION).apply();

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
      if (uri == null) {
         Bundle bundle = intent.getExtras();
         if (bundle != null) {
            Object objectUri = bundle.get(Intent.EXTRA_STREAM);
            if (objectUri != null) {
               uri = (Uri) objectUri;
            }
         }
      }
      if (uri != null) {
         handleUri(uri);
      }

      // if token is not expired, then skip the login module
      if (!UserUtility.getInstance(getApplicationContext()).isTokenExpired()) {
         skipLogin();
      } else {
         // temporarily prune complete work on every login to ensure our unique work is rescheduled
         WorkManager.getInstance(getApplicationContext()).pruneWork();
         application.stopLocationService();
      }

      // no title bar
      setContentView(R.layout.activity_login);
      hideKeyboardOnClick(findViewById(R.id.login));

      TextView appName = findViewById(R.id.mage);
      appName.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/GondolaMage-Regular.otf"));

      ((TextView) findViewById(R.id.login_version)).setText("App Version: " + preferences.getString(getString(R.string.buildVersionKey), "NA"));

      mServerURL = findViewById(R.id.server_url);

      String serverUrl = preferences.getString(getString(R.string.serverURLKey), getString(R.string.serverURLDefaultValue));
      if (StringUtils.isEmpty(serverUrl)) {
         changeServerURL();
         return;
      }

      findViewById(R.id.server_url).setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View v) {
            changeServerURL();
         }
      });

      mServerURL.setText(serverUrl);

      // Setup login based on last api pull
      configureLogin();

      viewModel = new ViewModelProvider(this).get(LoginViewModel.class);
      viewModel.getApiStatus().observe(this, valid -> observeApi());

      viewModel.getAuthenticationState().observe(this, this::observeAuthenticationState);
      viewModel.getAuthenticationStatus().observe(this, this::observeAuthentication);
      viewModel.getAuthorizationStatus().observe(this, this::observeAuthorization);

      viewModel.api(serverUrl);
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


   private void observeApi() {
      configureLogin();
   }

   private void observeAuthenticationState(LoginViewModel.AuthenticationState state) {
      if (state == LoginViewModel.AuthenticationState.LOADING) {
         findViewById(R.id.login_status).setVisibility(View.VISIBLE);
         findViewById(R.id.login_form).setVisibility(View.GONE);
      } else if (state == LoginViewModel.AuthenticationState.ERROR) {
         findViewById(R.id.login_status).setVisibility(View.GONE);
         findViewById(R.id.login_form).setVisibility(View.VISIBLE);
      }
   }

   private void observeAuthentication(Authentication authentication) {
      if (authentication == null) return;

      AuthenticationStatus status = authentication.getStatus();

      if (status.getStatus() == AuthenticationStatus.Status.SUCCESSFUL_AUTHENTICATION) {
         viewModel.authorize(authentication.getStrategy(), status.getToken(), status);
      } else if (status.getStatus() == AuthenticationStatus.Status.DISCONNECTED_AUTHENTICATION) {
         new AlertDialog.Builder(this)
                 .setTitle("Disconnected Login")
                 .setMessage("You are logging into MAGE in disconnected mode.  You must re-establish a connection in order to push and pull information to and from your server.")
                 .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    loginComplete(false);
                    dialog.dismiss();
                 })
                 .setCancelable(false)
                 .show();
      } else if (status.getStatus() == AuthenticationStatus.Status.ACCOUNT_CREATED) {
         String message = status.getMessage();
         if (message == null) {
            message = "Please contact a MAGE administrator to activate your account.";
         }

         new AlertDialog.Builder(this)
                 .setTitle("Account Created")
                 .setMessage(message)
                 .setPositiveButton(android.R.string.ok, null)
                 .show();
      } else {
         new AlertDialog.Builder(this)
                 .setTitle("Signin Failed")
                 .setMessage(status.getMessage())
                 .setPositiveButton(android.R.string.ok, null)
                 .show();
      }
   }

   private void observeAuthorization(LoginViewModel.Authorization authorization) {
      if (authorization == null) return;

      AuthorizationStatus.Status status = authorization.getStatus().getStatus();
      if (status == AuthorizationStatus.Status.SUCCESSFUL_AUTHORIZATION) {
         loginComplete(authorization.getUserChanged());
      } else if (status == AuthorizationStatus.Status.FAILED_AUTHORIZATION) {
         new AlertDialog.Builder(this)
                 .setTitle("Registration Sent")
                 .setMessage(R.string.device_registered_text)
                 .setPositiveButton(android.R.string.ok, null)
                 .show();
      } else if (status == AuthorizationStatus.Status.INVALID_SERVER) {
         new AlertDialog.Builder(this)
                 .setTitle("Application Compatibility Error")
                 .setMessage("This app is not compatible with this server. Please update your context or talk to your MAGE administrator.")
                 .setPositiveButton(android.R.string.ok, null)
                 .show();
      } else {
         new AlertDialog.Builder(this)
                 .setTitle("Signin Failed")
                 .setMessage(authorization.getStatus().getMessage())
                 .setPositiveButton(android.R.string.ok, null)
                 .show();
      }
   }

   private void configureLogin() {
      FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

      Map<String, JSONObject> strategies = new TreeMap<>();

      // TODO marshal authentication strategies to POJOs with Jackson
      JSONObject authenticationStrategies = PreferenceHelper.getInstance(getApplicationContext()).getAuthenticationStrategies();
      Iterator<String> iterator = authenticationStrategies.keys();
      while (iterator.hasNext()) {
         String strategyKey = iterator.next();
         try {
            JSONObject strategy = (JSONObject) authenticationStrategies.get(strategyKey);
            if ("local".equals(strategyKey)) {
               strategy.putOpt("type", strategyKey);
            }

            strategies.put(strategyKey, strategy);
         } catch (JSONException e) {
            e.printStackTrace();
         }
      }

      if (strategies.size() > 1 && strategies.containsKey("local")) {
         findViewById(R.id.or).setVisibility(View.VISIBLE);
      } else {
         findViewById(R.id.or).setVisibility(View.GONE);
      }

      findViewById(R.id.google_login_button).setVisibility(View.GONE);
      for (final Map.Entry<String, JSONObject> entry : strategies.entrySet()) {
         String authenticationName = entry.getKey();
         String authenticationType = entry.getValue().optString("type");

         if (getSupportFragmentManager().findFragmentByTag(authenticationName) != null) continue;

         if ("local".equals(authenticationName)) {
            Fragment loginFragment = MageLoginFragment.Companion.newInstance(entry.getKey(), entry.getValue());
            transaction.add(R.id.local_auth, loginFragment, authenticationName);
         } else if ("oauth".equals(authenticationType)) {
            Fragment loginFragment = IdpLoginFragment.Companion.newInstance(entry.getKey(), entry.getValue());
            transaction.add(R.id.third_party_auth, loginFragment, authenticationName);
         } else if ("saml".equals(authenticationType)) {
            Fragment loginFragment = IdpLoginFragment.Companion.newInstance(entry.getKey(), entry.getValue());
            transaction.add(R.id.third_party_auth, loginFragment, authenticationName);
         } else if ("ldap".equals(authenticationType)) {
            Fragment loginFragment = LdapLoginFragment.Companion.newInstance(entry.getKey(), entry.getValue());
            transaction.add(R.id.third_party_auth, loginFragment, authenticationName);
         }
      }

      // Remove authentication fragments that have been removed from server
      for (Fragment fragment: getSupportFragmentManager().getFragments()) {
         if (!strategies.keySet().contains(fragment.getTag())) {
            transaction.remove(fragment);
         }
      }

      transaction.commit();
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
    *
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
    * Fired when user clicks signup
    */
   public void signup(View view) {
      Intent intent;
      if (Compatibility.Companion.isServerVersion5(getApplicationContext())) {
         intent = new Intent(getApplicationContext(), SignupActivity_server5.class);

      } else {
         intent = new Intent(getApplicationContext(), SignupActivity.class);
      }

      startActivity(intent);
      finish();
   }

   private void loginComplete(Boolean userChanged) {
      boolean preserveActivityStack = !userChanged && mContinueSession;
      startNextActivityAndFinish(preserveActivityStack);
   }

   public void startNextActivityAndFinish(boolean preserveActivityStack) {
      // Continue session if there are other activities on the stack
      if (preserveActivityStack && !isTaskRoot()) {
         // We are going to return user to the app where they last left off,
         // make sure to start up MAGE services
         application.onLogin();

         // TODO look at refreshing the event here...
      } else {
         SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
         boolean showDisclaimer = sharedPreferences.getBoolean(getString(R.string.serverDisclaimerShow), false);

         Intent intent = showDisclaimer ?
                 new Intent(getApplicationContext(), DisclaimerActivity.class) :
                 new Intent(getApplicationContext(), EventsActivity.class);

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
                 new Intent(getApplicationContext(), EventsActivity.class) :
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
         application.onLogout(true, null);
      }
   }

   private void showSessionExpiredDialog() {
      AlertDialog dialog = new AlertDialog.Builder(this)
              .setTitle("Session Expired")
              .setCancelable(false)
              .setMessage("We apologize, but it looks like your MAGE session has expired.  Please login and we will take you back to what you were doing.")
              .setPositiveButton(android.R.string.ok, null).create();

      dialog.setCanceledOnTouchOutside(false);

      dialog.show();
   }
}
