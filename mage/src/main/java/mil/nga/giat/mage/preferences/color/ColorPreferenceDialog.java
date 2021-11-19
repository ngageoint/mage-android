package mil.nga.giat.mage.preferences.color;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.preference.DialogPreference;
import androidx.preference.PreferenceDialogFragmentCompat;

import com.google.android.material.textfield.TextInputLayout;

import org.jetbrains.annotations.NotNull;

import mil.nga.giat.mage.R;

public class ColorPreferenceDialog extends PreferenceDialogFragmentCompat implements View.OnTouchListener {

    private final ColorPickerPreference preference;
    private String chosenColor;
    private TextInputLayout hexTextLayout;

    public ColorPreferenceDialog(ColorPickerPreference preference) {
        this.preference = preference;
        final Bundle b = new Bundle();
        b.putString(ARG_KEY, preference.getKey());
        setArguments(b);
    }

    @Override
    protected View onCreateDialogView(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        return inflater.inflate(R.layout.dialog_color_picker, null);
    }

    private void addTouchListener(View parent, int viewId) {
        View redView = parent.findViewById(viewId);
        redView.setOnTouchListener(this);
    }

    @NonNull
    @NotNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Context context = getActivity();
        ConstraintLayout linearLayout = (ConstraintLayout) onCreateDialogView(context);
        hexTextLayout = linearLayout.findViewById(R.id.hexInputLayout);
        hexTextLayout.getEditText().addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence cs, int arg1, int arg2, int arg3) { }

            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) { }

            @Override
            public void afterTextChanged(Editable arg0) {
                String hexValue = hexTextLayout.getEditText().getText().toString();
                try {
                    int color = Color.parseColor(hexValue);
                    hexTextLayout.getEditText().getCompoundDrawables()[0].setTint(color);
                    chosenColor = hexValue;
                } catch (IllegalArgumentException ignored) { }
            }
        });

        addTouchListener(linearLayout, R.id.orange_tile);
        addTouchListener(linearLayout, R.id.red_tile);
        addTouchListener(linearLayout, R.id.blue_tile);
        addTouchListener(linearLayout, R.id.green_tile);
        addTouchListener(linearLayout, R.id.indigo_tile);
        addTouchListener(linearLayout, R.id.orange_tile);
        addTouchListener(linearLayout, R.id.pink_tile);
        addTouchListener(linearLayout, R.id.purple_tile);
        addTouchListener(linearLayout, R.id.red_tile);
        addTouchListener(linearLayout, R.id.light_blue_tile);
        addTouchListener(linearLayout, R.id.yellow_tile);
        addTouchListener(linearLayout, R.id.light_gray_tile);
        addTouchListener(linearLayout, R.id.dark_gray_tile);
        addTouchListener(linearLayout, R.id.black_tile);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        DialogPreference preference = getPreference();
        builder.setTitle(preference.getDialogTitle())
                .setPositiveButton(preference.getPositiveButtonText(), this)
                .setNegativeButton(preference.getNegativeButtonText(), this)
                .setView(linearLayout);

        return builder.create();
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if (positiveResult && chosenColor != null && !chosenColor.equalsIgnoreCase("")) {
            if (preference.callChangeListener(chosenColor)) {
                preference.setColor(chosenColor);
            }
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int color = v.getBackgroundTintList().getDefaultColor();
        chosenColor = String.format("#%06X", (0xFFFFFF & color));
        hexTextLayout.getEditText().setText(chosenColor);
        hexTextLayout.getEditText().getCompoundDrawables()[0].setTint(color);
        return true;
    }
}
