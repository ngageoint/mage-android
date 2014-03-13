package mil.nga.giat.mage.preferences;

import mil.nga.giat.mage.R;
import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;

public class MapViewPreference extends DialogPreference {

	public MapViewPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		setDialogLayoutResource(R.layout.base_layers);
	}
	
	@Override
	protected void onDialogClosed(boolean positiveResult) {
		super.onDialogClosed(positiveResult);
		
		
	}
}
