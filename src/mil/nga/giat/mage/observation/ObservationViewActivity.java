package mil.nga.giat.mage.observation;

import mil.nga.giat.mage.R;
import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

public class ObservationViewActivity extends FragmentActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.observation_viewer);
		this.setTitle("Suspicious Individual");
	}
}
