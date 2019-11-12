package mil.nga.giat.mage.observation;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import mil.nga.giat.mage.R;

/**
 * Created by wnewman on 8/22/16.
 */
public class ImportantDialog extends DialogFragment {

    private static String DESCRIPTION = "DESCRIPTION";

    public interface OnImportantListener {
        void onImportant(String description);
    }

    private OnImportantListener onImportantListener;

    // Empty constructor required for DialogFragment
    public ImportantDialog() {
    }

    public static ImportantDialog newInstance(String description) {
        ImportantDialog dialog = new ImportantDialog();
        Bundle args = new Bundle();
        args.putString(DESCRIPTION, description);
        dialog.setArguments(args);

        return dialog;
    }

    public void setOnImportantListener(OnImportantListener onImportantListener) {
        this.onImportantListener = onImportantListener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.fragment_observation_important, null);

        String description = getArguments().getString(DESCRIPTION);
        final TextView descriptionView = (TextView) view.findViewById(R.id.description);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setTitle("Important Observation")
                .setView(view);

        descriptionView.setText(description);

        String positiveText = description == null ? "Flag as Important" : "Update";
        builder.setPositiveButton(positiveText, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (onImportantListener != null) {
                            onImportantListener.onImportant(descriptionView.getText().toString());
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel, null);


        return builder.create();
    }
}
