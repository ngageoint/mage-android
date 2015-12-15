package mil.nga.giat.mage.sdk.preferences;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import mil.nga.giat.mage.sdk.R;


public class IntegerListValuePreference extends IntegerListPreference {

	private TextView value;

	public IntegerListValuePreference(Context context) {
		super(context);
	}

	public IntegerListValuePreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected View onCreateView(ViewGroup parent) {
		View view = super.onCreateView(parent);
		ViewGroup widgetFrame = (ViewGroup) view.findViewById(android.R.id.widget_frame);

		final LayoutInflater layoutInflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		if (widgetFrame != null) {
			layoutInflater.inflate(R.layout.preference_list_value, widgetFrame);
			widgetFrame.setVisibility(View.VISIBLE);
		}

		return view;
	}

	@Override
	protected void onBindView(View view) {
		super.onBindView(view);
		value = (TextView) view.findViewById(R.id.preferenceListValue);
		setListValue(getEntry());
	}

	public void setListValue(CharSequence text) {
		if (value != null) value.setText(text);
	}
}
