package mil.nga.giat.mage.observation;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.appcompat.app.AlertDialog;

import mil.nga.giat.mage.R;

/**
 * Created by wnewman on 8/22/16.
 */
public class ImportantRemoveDialog extends DialogFragment {

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

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.fragment_observation_remove_important, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setTitle("Remove Important Observation Flag")
                .setView(view);

        builder.setPositiveButton("Remove Important Flag", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (onRemoveImportantListener != null) {
                            onRemoveImportantListener.onRemoveImportant();
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel, null);

        return builder.create();
    }
}
