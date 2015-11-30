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

import org.apache.commons.lang3.StringUtils;

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
		String disclaimerText = sharedPreferences.getString(getString(R.string.serverDisclaimerText), null);
		if(disclaimerText == null) {
			disclaimerText = "";
		}
		sharedPreferences.edit().putString(getString(R.string.disclaimerText), disclaimerText).commit();

		if (StringUtils.isBlank(sharedPreferences.getString(getString(R.string.disclaimerText), null))) {
			agree(null);
		} else {
			TextView disclaimerTitleView = (TextView) findViewById(R.id.disclaimer_text);
			disclaimerTitleView.setText(disclaimerText);
		}

		String disclaimerTitle = sharedPreferences.getString(getString(R.string.serverDisclaimerTitle), null);
		if (disclaimerTitle != null) {
			TextView disclaimerTitleView = (TextView) findViewById(R.id.disclaimer_title);
			disclaimerTitleView.setText(disclaimerTitle);
		}
	}

	public void agree(View view) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		sharedPreferences.edit().putBoolean(getString(R.string.disclaimerAcceptedKey), true).commit();

		Intent intent = new Intent(getApplicationContext(), EventActivity.class);
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			intent.putExtras(extras);
		}
		startActivity(intent);
		finish();
	}

	public void exit(View view) {
		((MAGE) getApplication()).onLogout(true, new MAGE.OnLogoutListener() {
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