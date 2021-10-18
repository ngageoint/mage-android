package mil.nga.giat.mage.login;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import com.google.android.material.textfield.TextInputLayout;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;
import mil.nga.giat.mage.R;
import mil.nga.giat.mage.network.Resource;
import mil.nga.giat.mage.sdk.datastore.observation.AttachmentHelper;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationHelper;

/**
 *
 * Created by wnewman on 1/3/18.
 */
@AndroidEntryPoint
public class ServerUrlActivity extends AppCompatActivity {

	private View apiStatusView;
	private View serverUrlForm;
	private EditText serverUrlTextView;
	private TextInputLayout serverUrlLayout;
	private Button serverUrlButton;

	private ServerUrlViewModel viewModel;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_server_url);

		TextView appName = findViewById(R.id.mage);
		appName.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/GondolaMage-Regular.otf"));

		apiStatusView = findViewById(R.id.api_status);
		serverUrlForm = findViewById(R.id.server_url_form);

		serverUrlLayout = findViewById(R.id.server_url_layout);

		serverUrlTextView = findViewById(R.id.server_url);
		Button cancelButton =  findViewById(R.id.cancel_button);
		cancelButton.setOnClickListener(v -> done());

		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		String serverUrl = sharedPreferences.getString(getString(R.string.serverURLKey), getString(R.string.serverURLDefaultValue));
		if (StringUtils.isNoneEmpty(serverUrl)) {
			serverUrlTextView.setText(serverUrl);
		} else {
			// Don't let user cancel if no URL has been set.
			cancelButton.setVisibility(View.GONE);
		}
		serverUrlTextView.setSelection(serverUrlTextView.getText().length());

		serverUrlButton = findViewById(R.id.server_url_button);
		serverUrlButton.setOnClickListener(v -> onChangeServerUrl());

		viewModel = new ViewModelProvider(this).get(ServerUrlViewModel.class);
		viewModel.getApi().observe(this, this::onApi);
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
					.setPositiveButton("Continue", (dialog, which) -> changeServerURL())
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
		viewModel.setUrl(url);
	}

	public void onApi(Resource<Boolean> resource) {
		if (resource.getStatus() == Resource.Status.LOADING) {
			serverUrlButton.setEnabled(false);
			apiStatusView.setVisibility(View.VISIBLE);
			serverUrlForm.setVisibility(View.GONE);
		} else {
			if (resource.getStatus() == Resource.Status.SUCCESS) {
				if (resource.getData() != null) {
					done();
				} else {
					new AlertDialog.Builder(this)
						.setTitle("Compatibility Error")
						.setMessage("Your MAGE application is not compatible with this server.  Please update your application or contact your MAGE administrator for support.")
						.setPositiveButton(android.R.string.ok, null)
						.create()
						.show();

					serverUrlLayout.setError("Application is not compatible with server.");
				}
			} else {
				apiStatusView.setVisibility(View.GONE);
				serverUrlForm.setVisibility(View.VISIBLE);
				serverUrlButton.setEnabled(true);
				serverUrlLayout.setError(resource.getMessage());
			}
		}
	}

	private void done() {
		// finish this activity back to the login activity
		Intent intent = new Intent(this, LoginActivity.class);
		startActivity(intent);
		finish();
	}
}
