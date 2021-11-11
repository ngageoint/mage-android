package mil.nga.giat.mage.observation;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;

import mil.nga.giat.mage.R;

/**
 * Created by wnewman on 8/22/16.
 */
public class ImportantRemoveDialog extends AppCompatDialogFragment {

    public interface OnRemoveImportantListener {
        void onRemoveImportant();
    }

    private OnRemoveImportantListener onRemoveImportantListener;

    // Empty constructor required for DialogFragment
    public ImportantRemoveDialog() {
    }

    public void setOnRemoveImportantListener(OnRemoveImportantListener onRemoveImportantListener) {
        this.onRemoveImportantListener = onRemoveImportantListener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.fragment_observation_remove_important, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setTitle("Remove Important Flag")
                .setView(view);

        builder.setPositiveButton("Remove Flag", (dialog, which) -> {
            if (onRemoveImportantListener != null) {
                onRemoveImportantListener.onRemoveImportant();
            }
        })
                .setNegativeButton(android.R.string.cancel, null);

        return builder.create();
    }
}
