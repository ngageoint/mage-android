package mil.nga.giat.mage.map.preference;

import java.util.HashSet;
import java.util.Set;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.Preference;
import android.util.AttributeSet;

public class OverlayPreference extends Preference {

    public static final String OVERLAY_EXTENDED_DATA_KEY = "overlay";
    public static final int OVERLAY_ACTIVITY = 0;
    
    private Set<String> mValues = new HashSet<String>();

    public OverlayPreference(Context context) {
        super(context);
    }
    
    public OverlayPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Sets the value of the key. This should contain entries in
     * {@link #getEntryValues()}.
     * 
     * @param values
     *            The values to set for the key.
     */
    public void setValues(Set<String> values) {
        mValues.clear();
        mValues.addAll(values);

        persistStringSet(values);
    }

    /**
     * Retrieves the current value of the key.
     */
    public Set<String> getValues() {
        return mValues;
    }

    @Override
    protected void onClick() {
        Activity activity = (Activity) getContext();
        Intent intent = new Intent(activity, OverlayPreferenceActivity.class);
        activity.startActivityForResult(intent, OVERLAY_ACTIVITY);
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        setValues(restoreValue ? getPersistedStringSet(mValues) : (Set<String>) defaultValue);
    }
    
    protected Set<String> getPersistedStringSet(Set<String> defaultReturnValue) {
        if (!shouldPersist()) {
            return defaultReturnValue;
        }
        
        return getPreferenceManager().getSharedPreferences().getStringSet(getKey(), defaultReturnValue);
    }
    
    protected boolean persistStringSet(Set<String> values) {
        if (shouldPersist()) {
            // Shouldn't store null
            if (values.equals(getPersistedStringSet(null))) {
                // It's already there, so the same as persisting
                return true;
            }
            
            SharedPreferences.Editor editor = getPreferenceManager().getSharedPreferences().edit();
            editor.putStringSet(getKey(), values);
            editor.apply();
            return true;
        }
        return false;
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        if (isPersistent()) {
            // No need to save instance state
            return superState;
        }

        final SavedState myState = new SavedState(superState);
        myState.values = getValues();
        return myState;
    }

    private static class SavedState extends BaseSavedState {
        Set<String> values;

        public SavedState(Parcel source) {
            super(source);
            values = new HashSet<String>();
            String[] strings = null;
            int length = source.readInt();
            if (length >= 0)
            {
                strings = new String[length];

                for (int i = 0 ; i < length ; i++)
                {
                    strings[i] = source.readString();
                }
            }   

            final int stringCount = strings.length;
            for (int i = 0; i < stringCount; i++) {
                values.add(strings[i]);
            }
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeStringArray(values.toArray(new String[0]));
        }

        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
}