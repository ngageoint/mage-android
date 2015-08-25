package mil.nga.giat.mage.disclaimer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import mil.nga.giat.mage.MAGE;
import mil.nga.giat.mage.R;
import mil.nga.giat.mage.event.EventActivity;
import mil.nga.giat.mage.login.LoginActivity;

public class DisclaimerActivity extends FragmentActivity {

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
		String disclaimerTitle = sharedPreferences.getString(getString(R.string.serverDisclaimerTitle), null);
		if (disclaimerTitle != null) {
			TextView disclaimerTitleView = (TextView) findViewById(R.id.disclaimer_title);
			disclaimerTitleView.setText(disclaimerTitle);
		}

		String disclaimerText = sharedPreferences.getString(getString(R.string.serverDisclaimerText), null);
		if (disclaimerText != null) {
			TextView disclaimerTextView = (TextView) findViewById(R.id.disclaimer_text);
			disclaimerTextView.setText(disclaimerText);
		}
	}

	public void agree(View view) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putBoolean(getString(R.string.disclaimerAccepted), true);
		editor.apply();

		Intent intent = new Intent(getApplicationContext(), EventActivity.class);
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			intent.putExtras(extras);
		}
		startActivity(intent);
		finish();
	}

	public void exit(View view) {
		((MAGE)getApplication()).onLogout(true);
		startActivity(new Intent(getApplicationContext(), LoginActivity.class));
		finish();
		return;
	}

	@Override
	public void onBackPressed() {
	}
}