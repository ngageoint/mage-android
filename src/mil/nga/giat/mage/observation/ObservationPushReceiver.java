package mil.nga.giat.mage.observation;

import mil.nga.giat.mage.sdk.datastore.observation.Observation;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationHelper;
import mil.nga.giat.mage.sdk.exceptions.ObservationException;
import mil.nga.giat.mage.sdk.service.ObservationIntentService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class ObservationPushReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		Long primaryKey = intent.getLongExtra(ObservationIntentService.OBSERVATION_ID, -1);
		Observation o;
		try {
			o = ObservationHelper.getInstance(context).readByPrimaryKey(primaryKey);
			if (o.getAttachments().size() != 0) {
				Toast.makeText(context, "Pushed observation " + o.getId(), Toast.LENGTH_SHORT).show();
			}
		} catch (ObservationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
