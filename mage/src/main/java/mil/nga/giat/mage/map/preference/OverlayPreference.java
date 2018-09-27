package mil.nga.giat.mage.map.preference;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class OverlayPreference extends Preference {

    private Drawable downloadIcon;
    private TextView titleView;
    private Set<String> overlays = new HashSet<>();

    public OverlayPreference(Context context) {
        super(context);
    }

    public OverlayPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Sets the value of the key.
     *
     * @param values
     *            The values to set for the key.
     */
    public void setValues(Set<String> values) {
        overlays.clear();
        overlays.addAll(values);

        persistStringSet(values);
    }

    /**
     * Retrieves the current value of the key.
     */
    public Set<String> getValues() {
        return overlays;
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        titleView = (TextView) holder.findViewById(android.R.id.title);
        titleView.setCompoundDrawablePadding(20);
        titleView.setCompoundDrawablesWithIntrinsicBounds(null, null, downloadIcon, null);
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        setValues(restoreValue ? getPersistedStringSet(overlays) : (Set<String>) defaultValue);
    }

    public void setDownloadIcon(Drawable downloadIcon) {
        this.downloadIcon = downloadIcon;
        notifyChanged();
    }

    public Set<String> getPersistedStringSet(Set<String> defaultReturnValue) {
        if (!shouldPersist()) {
            return defaultReturnValue;
        }
        
        return getPreferenceManager().getSharedPreferences().getStringSet(getKey(), defaultReturnValue);
    }
    
    public boolean persistStringSet(Set<String> values) {
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

        SavedState(Parcel source) {
            super(source);
            values = new HashSet<>();
			int length = source.readInt();
            if (length >= 0) {
				List<String> strings = new ArrayList<String>(length);

                for (int i = 0 ; i < length ; i++) {
                    strings.add(source.readString());
                }

				values.addAll(strings);
            }
        }

        SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeStringArray(values.toArray(new String[values.size()]));
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