package mil.nga.giat.mage;

import mil.nga.giat.mage.login.LoginActivity;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;

public class DisclaimerActivity extends FragmentActivity implements OnClickListener {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_disclaimer);

		findViewById(R.id.disclaimerExit).setOnClickListener(this);
		findViewById(R.id.disclaimerAgree).setOnClickListener(this);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.landing, menu);
		return true;
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
			case R.id.disclaimerExit: {
				Intent intent = new Intent(Intent.ACTION_MAIN);
				intent.addCategory(Intent.CATEGORY_HOME);
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(intent);
				break;
			}
			case R.id.disclaimerAgree: {
				Editor editor = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
				editor.putBoolean("disclaimerAgree", true);
				editor.apply();
				
				Intent intent = new Intent(this, LoginActivity.class);
				startActivity(intent);
				break;
			}
		}
	}
}