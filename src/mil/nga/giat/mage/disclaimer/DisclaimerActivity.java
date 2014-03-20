package mil.nga.giat.mage.disclaimer;

import mil.nga.giat.mage.R;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.Window;

public class DisclaimerActivity extends FragmentActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_disclaimer);
	}

	public void agree(View view) {
		Editor editor = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
		editor.putString("showDisclaimer", Boolean.FALSE.toString()).commit();
		finish();
	}

	public void exit(View view) {
		Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_HOME);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
	}
}