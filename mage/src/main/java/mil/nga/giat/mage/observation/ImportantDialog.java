package mil.nga.giat.mage.observation;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationImportant;

/**
 * Created by wnewman on 8/22/16.
 */
public class ImportantDialog extends AppCompatDialogFragment {

    private static String DESCRIPTION = "DESCRIPTION";

    public interface OnImportantListener {
        void onImportant(String description);
    }

    private OnImportantListener onImportantListener;

    // Empty constructor required for DialogFragment
    public ImportantDialog() {
    }

    public static ImportantDialog newInstance(@Nullable String description) {
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
        final TextView descriptionView = view.findViewById(R.id.description);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setTitle("Important Observation")
                .setView(view);

        descriptionView.setText(description);

        String positiveText = description == null ? "Flag as Important" : "Update";
        builder.setPositiveButton(positiveText, (dialog, which) -> {
            if (onImportantListener != null) {
                onImportantListener.onImportant(descriptionView.getText().toString());
            }
        }).setNegativeButton(android.R.string.cancel, null);


        return builder.create();
    }
}
