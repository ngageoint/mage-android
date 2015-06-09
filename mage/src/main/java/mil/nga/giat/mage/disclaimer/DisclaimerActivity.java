package mil.nga.giat.mage.disclaimer;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import mil.nga.giat.mage.MAGE;
import mil.nga.giat.mage.R;

public class DisclaimerActivity extends FragmentActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_disclaimer);
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		((MAGE) getApplication()).onLogout(true);
	}

	public void agree(View view) {
		finish();
	}

	public void exit(View view) {
		((MAGE) getApplication()).onLogout(true);
		Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_HOME);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
	}

	@Override
	public void onBackPressed() {
	}
}