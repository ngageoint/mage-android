package mil.nga.giat.mage.sdk.preferences;

import android.content.Context;
import android.preference.EditTextPreference;
import android.util.AttributeSet;

/**
 * Created by wnewman on 12/15/15.
 */
public class EditTextSummaryPreference extends EditTextPreference {
    public EditTextSummaryPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public EditTextSummaryPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EditTextSummaryPreference(Context context) {
        super(context);
    }

    @Override
    public CharSequence getSummary() {
        String summary = super.getSummary().toString();
        return String.format(summary, getText());
    }
}
