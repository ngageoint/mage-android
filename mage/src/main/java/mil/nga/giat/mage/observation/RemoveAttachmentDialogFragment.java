package mil.nga.giat.mage.observation;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

import mil.nga.giat.mage.R;

public class RemoveAttachmentDialogFragment extends DialogFragment {
	
    public interface RemoveAttachmentDialogListener {
        public void onDialogPositiveClick(DialogFragment dialog);
        public void onDialogNegativeClick(DialogFragment dialog);
    }
    
    RemoveAttachmentDialogListener listener;
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            listener = (RemoveAttachmentDialogListener) activity;
        } catch (ClassCastException e) {
        }
    }
	
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.remove_attachment_title)
        		.setIcon(R.drawable.ic_delete_black_24dp)
        		.setMessage(R.string.remove_attachment_dialog_message)
               .setPositiveButton(R.string.remove_attachment_confirm, new DialogInterface.OnClickListener() {
				@Override
                   public void onClick(DialogInterface dialog, int id) {
					   if (listener != null) listener.onDialogPositiveClick(RemoveAttachmentDialogFragment.this);
                   }
               })
               .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                	   if (listener != null) listener.onDialogNegativeClick(RemoveAttachmentDialogFragment.this);
                       RemoveAttachmentDialogFragment.this.getDialog().cancel();
                   }
               });

        // Create the AlertDialog object and return it
        return builder.create();
    }

}
