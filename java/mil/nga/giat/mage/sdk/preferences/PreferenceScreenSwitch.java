package mil.nga.giat.mage.sdk.preferences;

import android.content.Context;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.SwitchPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;

public class PreferenceScreenSwitch extends SwitchPreference implements OnPreferenceChangeListener {

    public PreferenceScreenSwitch(Context context, AttributeSet attrs) {
        super(context, attrs);
        
        setOnPreferenceChangeListener(this);
    }
    
    @Override
    protected void onClick() {
        // Don't let users click the actual preference to changed the switch state
    }

    @Override
    public boolean onPreferenceChange(Preference p, Object value) {
        PreferenceScreenSwitch preference = (PreferenceScreenSwitch) p;
        Boolean checked = (Boolean) value;
        preference.setChecked(checked);        
        return true;
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        
        View widget = view.findViewById(android.R.id.widget_frame);
        
        ViewGroup group = (ViewGroup) widget;
        for (int i = 0; i < group.getChildCount(); ++i) {
            View child = group.getChildAt(i);
            if (child instanceof Switch) {
                View s = (Switch) child;
                s.setPadding(s.getPaddingLeft(), s.getPaddingTop(), 0, s.getPaddingBottom());
            }
        }
    }
    
    
}