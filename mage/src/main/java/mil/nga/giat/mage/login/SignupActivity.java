package mil.nga.giat.mage.login;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputLayout;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.sdk.login.SignupStatus;
import mil.nga.giat.mage.sdk.login.SignupTask;
import mil.nga.giat.mage.sdk.preferences.PreferenceHelper;

public class SignupActivity extends AppCompatActivity implements SignupTask.SignupDelegate {
	private static final String LOG_NAME = SignupActivity.class.getName();

	private EditText mDisplayNameEditText;
	private TextInputLayout mDisplayNameLayout;

	private EditText mUsernameEditText;
	private TextInputLayout mUsernameLayout;

	private EditText mEmailEditText;
	private TextInputLayout mEmailLayout;

	private EditText mPhoneEditText;

	private EditText mPasswordEditText;
	private TextInputLayout mPasswordLayout;

	private EditText mConfirmPasswordEditText;
	private TextInputLayout mConfirmPasswordLayout;

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

	public final EditText getPhoneEditText() {
		return mPhoneEditText;
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

		setContentView(R.layout.activity_signup);

		TextView appName = findViewById(R.id.mage);
		appName.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/GondolaMage-Regular.otf"));

		final PasswordStrengthFragment passwordStrengthFragment = (PasswordStrengthFragment) getSupportFragmentManager().findFragmentById(R.id.password_strength_fragment);

		mDisplayNameEditText = findViewById(R.id.signup_displayname);
		mDisplayNameLayout = findViewById(R.id.displayname_layout);

		mUsernameEditText = findViewById(R.id.signup_username);
		mUsernameLayout = findViewById(R.id.username_layout);

		mEmailEditText = findViewById(R.id.signup_email);
		mEmailLayout = findViewById(R.id.email_layout);

		mPhoneEditText = findViewById(R.id.signup_phone);

		mPasswordEditText = findViewById(R.id.signup_password);
		mPasswordEditText.setTypeface(Typeface.DEFAULT);
		mPasswordLayout = findViewById(R.id.password_layout);
		mPasswordEditText.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {

			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {

			}

			@Override
			public void afterTextChanged(Editable s) {
				List<String> sanitizedPasswordInputs = new ArrayList<>();
				sanitizedPasswordInputs.add(mDisplayNameEditText.getText().toString());
				sanitizedPasswordInputs.add(mUsernameEditText.getText().toString());
				sanitizedPasswordInputs.add(mEmailEditText.getText().toString());
				sanitizedPasswordInputs.removeAll(Collections.singleton(null));
				passwordStrengthFragment.setSanitizedList(sanitizedPasswordInputs);

				passwordStrengthFragment.onPasswordChanged(s.toString());
			}
		});

		mConfirmPasswordEditText = findViewById(R.id.signup_confirmpassword);
		mConfirmPasswordEditText.setTypeface(Typeface.DEFAULT);
		mConfirmPasswordLayout = findViewById(R.id.confirmpassword_layout);

		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		serverURL = sharedPreferences.getString(getString(R.string.serverURLKey), getString(R.string.serverURLDefaultValue));

		mConfirmPasswordEditText.setOnKeyListener((v, keyCode, event) -> {
			if (keyCode == KeyEvent.KEYCODE_ENTER) {
				signup(v);
				return true;
			} else {
				return false;
			}
		});

		configureSignup();
	}

	private void configureSignup() {
		PreferenceHelper preferenceHelper = PreferenceHelper.getInstance(getApplicationContext());
		if (preferenceHelper.containsLocalAuthentication()) {
			Button localButton = findViewById(R.id.local_signup_button);
			localButton.setOnClickListener(this::signup);
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
		mDisplayNameLayout.setError(null);
		mUsernameLayout.setError(null);
		mEmailLayout.setError(null);
		mPasswordLayout.setError(null);
		mConfirmPasswordLayout.setError(null);

		String displayName = getDisplayNameEditText().getText().toString();
		String username = getUsernameEditText().getText().toString();
		String email = getEmailEditText().getText().toString();
		String phone = getPhoneEditText().getText().toString();
		String password = getPasswordEditText().getText().toString();
		String confirmpassword = getConfirmPasswordEditText().getText().toString();

		// are the inputs valid?
		if (TextUtils.isEmpty(displayName)) {
			mDisplayNameLayout.setError("Display name can not be blank");
			return;
		}

		if (TextUtils.isEmpty(username)) {
			mUsernameLayout.setError("Username can not be blank");
			return;
		}

		// is email address the right syntax?
		if (StringUtils.isNotEmpty(email) && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
			mEmailLayout.setError("Please enter a valid email address");
			return;
		}

		if (TextUtils.isEmpty(password)) {
			mPasswordLayout.setError("Password can not be blank");
			return;
		}

		if (TextUtils.isEmpty(confirmpassword)) {
			mConfirmPasswordLayout.setError("Enter password again");
			return;
		}

		// do passwords match?
		if (!password.equals(confirmpassword)) {
			mPasswordLayout.setError("Passwords do not match");
			mConfirmPasswordLayout.setError("Passwords do not match");
			return;
		}

		List<String> accountInfo = new ArrayList<>();
		accountInfo.add(username);
		accountInfo.add(displayName);
		accountInfo.add(email);
		accountInfo.add(phone);
		accountInfo.add(password);
		accountInfo.add(serverURL);

		// show spinner, and hide form
		findViewById(R.id.signup_form).setVisibility(View.GONE);
		findViewById(R.id.signup_status).setVisibility(View.VISIBLE);

		new SignupTask(this.getApplicationContext(), this).execute(accountInfo.toArray(new String[accountInfo.size()]));
	}

	@Override
	public void onSignupComplete(SignupStatus status) {
		if (status.getStatus() == SignupStatus.Status.SUCCESSFUL_SIGNUP) {
			boolean isActive = status.getUser().get("active").getAsBoolean();

			// Tell the user that their account was made
			showSignupSuccessDialog(isActive);
		} else {
			new AlertDialog.Builder(this)
					.setTitle("Signup Failed")
					.setMessage(status.getMessage())
					.setPositiveButton(android.R.string.ok, null)
					.show();

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

	private void showSignupSuccessDialog(boolean isActive) {
		AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
		alertDialog.setTitle(R.string.account_inactive_title);

		if (isActive) {
			alertDialog.setMessage(getString(R.string.account_active_message));
		} else{
			alertDialog.setMessage(getString(R.string.account_inactive_message));
		}

		alertDialog.setPositiveButton("Ok", (dialog, which) -> login(null));
		alertDialog.show();
	}
}
