package mil.nga.giat.mage.login;


import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.util.Patterns;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceManager;

import com.google.android.material.textfield.TextInputLayout;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.contact.utilities.LinkGenerator;
import mil.nga.giat.mage.sdk.datastore.DaoStore;
import mil.nga.giat.mage.sdk.datastore.observation.AttachmentHelper;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationHelper;
import mil.nga.giat.mage.sdk.preferences.ServerApi;
import mil.nga.giat.mage.sdk.utils.DeviceUuidFactory;

/**
 *
 * Created by wnewman on 1/3/18.
 */
public class ServerUrlActivity extends AppCompatActivity implements ServerApi.ServerApiListener {

	private View apiStatusView;
	private View serverUrlForm;
	private EditText serverUrlTextView;
	private TextInputLayout serverUrlLayout;
	private Button serverUrlButton;

	private static final String SERVER_URL_FRAGMENT_TAG = "SERVER_URL_FRAGMENT";
	private ServerUrlFragment urlFragment;

	@Inject
	protected SharedPreferences preferences;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_server_url);

		TextView appName = (TextView) findViewById(R.id.mage);
		appName.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/GondolaMage-Regular.otf"));

		apiStatusView = findViewById(R.id.api_status);
		serverUrlForm = findViewById(R.id.server_url_form);

		serverUrlLayout = (TextInputLayout) findViewById(R.id.server_url_layout);

		serverUrlTextView = (EditText) findViewById(R.id.server_url);
		Button cancelButton = (Button) findViewById(R.id.cancel_button);
		cancelButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				done();
			}
		});

		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		String serverUrl = sharedPreferences.getString(getString(R.string.serverURLKey), getString(R.string.serverURLDefaultValue));
		if (StringUtils.isNoneEmpty(serverUrl)) {
			serverUrlTextView.setText(serverUrl);
		} else {
			// Don't let user cancel if no URL has been set.
			cancelButton.setVisibility(View.GONE);
		}
		serverUrlTextView.setSelection(serverUrlTextView.getText().length());

		serverUrlButton = (Button) findViewById(R.id.server_url_button);
		serverUrlButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onChangeServerUrl();
			}
		});


		FragmentManager fragmentManager = getSupportFragmentManager();
		urlFragment = (ServerUrlFragment) fragmentManager.findFragmentByTag(SERVER_URL_FRAGMENT_TAG);

		// If the Fragment is non-null, then it is being retained over a configuration change.
		if (urlFragment == null) {
			urlFragment = new ServerUrlFragment();
			fragmentManager.beginTransaction().add(urlFragment, SERVER_URL_FRAGMENT_TAG).commit();
		}

		if (urlFragment.isValidatingApi()) {
			serverUrlButton.setEnabled(false);
			apiStatusView.setVisibility(View.VISIBLE);
			serverUrlForm.setVisibility(View.GONE);
		}
	}

	private void onChangeServerUrl() {
		int unsavedObservations =  ObservationHelper.getInstance(getApplicationContext()).getDirty().size();
		int unsavedAttachments = AttachmentHelper.getInstance(getApplicationContext()).getDirtyAttachments().size();

		List<String> warnings = new ArrayList<>();
		if (unsavedObservations > 0) {
			warnings.add(String.format(Locale.getDefault(), "%d unsaved observations", unsavedObservations));
		}
		if (unsavedAttachments > 0) {
			warnings.add(String.format(Locale.getDefault(),"%d unsaved attachments", unsavedAttachments));
		}

		if (warnings.size() > 0) {
			new AlertDialog.Builder(this)
					.setTitle("You Have Unsaved Data")
					.setMessage(String.format("You have %s.  All unsaved observations will be lost if you continue.", StringUtils.join(warnings, " and ")))
					.setPositiveButton("Continue", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							changeServerURL();
						}
					})
					.setNegativeButton(android.R.string.cancel, null)
					.create()
					.show();
		} else {
			changeServerURL();
		}
	}

	private void changeServerURL() {
		String url = serverUrlTextView.getText().toString().trim();
		if (!Patterns.WEB_URL.matcher(url).matches()) {
			serverUrlLayout.setError("Invalid URL");
			return;
		}

		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(serverUrlTextView.getWindowToken(), 0);

		serverUrlTextView.setText(url);
		serverUrlButton.setEnabled(false);
		apiStatusView.setVisibility(View.VISIBLE);
		serverUrlForm.setVisibility(View.GONE);

		urlFragment.validateApi(url);
	}

	@Override
	public void onApi(boolean valid, Exception error) {
		if (valid) {
			// Clear the database
			final DaoStore daoStore = DaoStore.getInstance(getApplicationContext());
			if (!daoStore.isDatabaseEmpty()) {
				daoStore.resetDatabase();
			}

			SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
			SharedPreferences.Editor editor = sharedPreferences.edit();
			editor.putString(getString(R.string.serverURLKey), serverUrlTextView.getText().toString()).apply();

			// finish this activity back to the login activity
			done();
		} else {
			apiStatusView.setVisibility(View.GONE);
			serverUrlForm.setVisibility(View.VISIBLE);
			serverUrlButton.setEnabled(true);

			String message = "Cannot connect to server.";
			if (error == null) {
				message = "Your MAGE application is not compatible with this server.  Please update your application or contact your MAGE administrator for support.";

				final Spanned s = addLinks(message);
				final AlertDialog dialog = new AlertDialog.Builder(this)
						.setTitle("Compatibility Error")
						.setMessage(message)
						.setPositiveButton(android.R.string.ok, null)
						.create();
				dialog.show();
				((TextView) dialog.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
			} else {
				if (error.getCause() != null) {
					message = error.getCause().getMessage();
				}
			}

			serverUrlLayout.setError(message);
		}
	}

	private void done() {
		// finish this activity back to the login activity
		Intent intent = new Intent(this, LoginActivity.class);
		startActivity(intent);
		finish();
	}

	private Spanned addLinks(final String message) {
		final String identifier = new DeviceUuidFactory(getApplicationContext()).getDeviceUuid().toString();
		final String emailLink = LinkGenerator.getEmailLink(this.preferences, message, identifier, null);
		final String phoneLink = LinkGenerator.getPhoneLink(this.preferences);

		final Spanned s = Html.fromHtml(message + " <br /><br /> "
				+ "You may contact your MAGE administrator via <a href= "
				+ emailLink + ">Email</a> or <a href="
				+ phoneLink + ">Phone</a> for further assistance.");

		return s;
	}
}
