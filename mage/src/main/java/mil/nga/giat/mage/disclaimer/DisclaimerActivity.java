package mil.nga.giat.mage.disclaimer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import mil.nga.giat.mage.MageApplication;
import mil.nga.giat.mage.R;
import mil.nga.giat.mage.event.EventsActivity;
import mil.nga.giat.mage.login.LoginActivity;

@AndroidEntryPoint
public class DisclaimerActivity extends AppCompatActivity {

	@Inject
	protected MageApplication application;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		setContentView(R.layout.activity_disclaimer);
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
	}

	@Override
	protected void onResume() {
		super.onResume();

		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

		boolean showDisclaimer = sharedPreferences.getBoolean(getString(R.string.serverDisclaimerShow), false);

		if (showDisclaimer) {
			String disclaimerTitle = sharedPreferences.getString(getString(R.string.serverDisclaimerTitle), null);
			TextView disclaimerTitleView = findViewById(R.id.disclaimer_title);
			disclaimerTitleView.setText(disclaimerTitle);

			TextView disclaimerTextView = findViewById(R.id.disclaimer_text);
			String disclaimerText = sharedPreferences.getString(getString(R.string.serverDisclaimerText), null);
			disclaimerTextView.setText(disclaimerText);
		} else {
			agree(null);
		}
	}

	public void agree(View view) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		sharedPreferences.edit().putBoolean(getString(R.string.disclaimerAcceptedKey), true).apply();

		Intent intent = new Intent(getApplicationContext(), EventsActivity.class);
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			intent.putExtras(extras);
		}

		startActivity(intent);
		finish();
	}

	public void exit(View view) {
		application.onLogout(true, new MageApplication.OnLogoutListener() {
			@Override
			public void onLogout() {
				startActivity(new Intent(getApplicationContext(), LoginActivity.class));
				finish();
			}
		});
	}

	@Override
	public void onBackPressed() {
	}
}