package mil.nga.giat.mage.profile;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import com.google.gson.JsonObject;
import com.nulabinc.zxcvbn.Zxcvbn;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import dagger.android.support.DaggerAppCompatActivity;
import mil.nga.giat.mage.MageApplication;
import mil.nga.giat.mage.R;
import mil.nga.giat.mage.login.LoginActivity;
import mil.nga.giat.mage.login.PasswordStrengthFragment;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.datastore.user.UserHelper;
import mil.nga.giat.mage.sdk.exceptions.UserException;
import mil.nga.giat.mage.sdk.http.resource.UserResource;
import retrofit.Callback;
import retrofit.Response;
import retrofit.Retrofit;

/**
 * Created by wnewman on 12/14/17.
 */

public class ChangePasswordActivity extends DaggerAppCompatActivity {

    private static final String LOG_NAME = ChangePasswordActivity.class.getName();

    @Inject
    MageApplication application;

    private String username;

    private TextInputEditText password;
    private TextInputLayout passwordLayout;

    private TextInputEditText newPassword;
    private TextInputLayout newPasswordLayout;

    private TextInputEditText newPasswordConfirm;
    private TextInputLayout newPasswordConfirmLayout;

    private Zxcvbn zxcvbn = new Zxcvbn();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_change_password);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_close_white_24dp);

        final PasswordStrengthFragment passwordStrengthFragment = (PasswordStrengthFragment) getSupportFragmentManager().findFragmentById(R.id.password_strength_fragment);

        try {
            User user = UserHelper.getInstance(getApplicationContext()).readCurrentUser();
            username = user.getUsername();

            List<String> sanitizedPasswordInputs = new ArrayList<>();
            sanitizedPasswordInputs.add(user.getUsername());
            sanitizedPasswordInputs.add(user.getDisplayName());
            sanitizedPasswordInputs.add(user.getEmail());
            sanitizedPasswordInputs.removeAll(Collections.singleton(null));
            passwordStrengthFragment.setSanitizedList(sanitizedPasswordInputs);
        } catch (UserException ue) {
            Log.e(LOG_NAME, "Problem finding current user.", ue);
        }

        password = (TextInputEditText) findViewById(R.id.password);
        passwordLayout = (TextInputLayout) findViewById(R.id.password_layout);

        newPassword = (TextInputEditText) findViewById(R.id.new_password);
        newPasswordLayout = (TextInputLayout) findViewById(R.id.new_password_layout);

        newPasswordConfirm = (TextInputEditText) findViewById(R.id.new_password_confirm);
        newPasswordConfirmLayout = (TextInputLayout) findViewById(R.id.new_password_confirm_layout);

        newPassword = (TextInputEditText) findViewById(R.id.new_password);
        newPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                passwordStrengthFragment.onPasswordChanged(s.toString());
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
        }

        return true;
    }

    public void onChangePasswordClick(View v) {
        if (!validateInputs()) {
            return;
        }

        UserResource userResource = new UserResource(getApplicationContext());
        userResource.changePassword(username, password.getText().toString(), newPassword.getText().toString(), newPasswordConfirm.getText().toString(), new Callback<JsonObject>() {
            @Override
            public void onResponse(Response<JsonObject> response, Retrofit retrofit) {
                if (response.isSuccess()) {
                    onSuccess();
                } else {
                    onError(response);
                }
            }

            @Override
            public void onFailure(Throwable t) {
                onError(null);
            }
        });
    }

    private void onSuccess() {
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle("Password Changed")
            .setMessage("Your passsword has been changed, for security purposes you will need to login with your new password.")
            .setCancelable(false)
            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    application.onLogout(true, null);
                    Intent intent = new Intent(ChangePasswordActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }
            }).create();

        dialog.show();
    }

    private void onError(Response<JsonObject> response) {
        if (response == null) {
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle("No connection")
                    .setMessage("Please ensure you have an internet connection and try again")
                    .setCancelable(false)
                    .setPositiveButton(android.R.string.ok, null).create();

            dialog.show();
        } else {
            int errorCode = response.code();
            if (errorCode == 401) {
                passwordLayout.setError("Invalid password, please check your password and try again");
            } else {
                AlertDialog dialog = new AlertDialog.Builder(this)
                        .setTitle("Error changing password")
                        .setMessage(response.message())
                        .setCancelable(false)
                        .setPositiveButton(android.R.string.ok, null).create();

                dialog.show();
            }
        }
    }

    private boolean validateInputs() {
        passwordLayout.setError(null);
        newPasswordLayout.setError(null);
        newPasswordConfirmLayout.setError(null);

        if (StringUtils.isBlank(password.getText().toString())) {
            passwordLayout.setError("Password is required");
            return false;
        }

        if (StringUtils.isBlank(newPassword.getText().toString())) {
            newPasswordLayout.setError("New Password is required");
            return false;
        }

        if (!newPassword.getText().toString().equals(newPasswordConfirm.getText().toString())) {
            newPasswordLayout.setError("Passwords do not match");
            newPasswordConfirmLayout.setError("Passwords do not match");
            return false;
        }

        if (password.getText().toString().equals(newPassword.getText().toString())) {
            newPasswordLayout.setError("New password cannot be the same as current password.");
            newPasswordConfirmLayout.setError("New password cannot be the same as current password.");
            return false;
        }

        return true;
    }

}
