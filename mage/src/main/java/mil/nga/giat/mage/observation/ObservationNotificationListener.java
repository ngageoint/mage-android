package mil.nga.giat.mage.observation;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.Collection;

import mil.nga.giat.mage.LandingActivity;
import mil.nga.giat.mage.MageApplication;
import mil.nga.giat.mage.R;
import mil.nga.giat.mage.database.model.observation.Observation;
import mil.nga.giat.mage.sdk.event.IObservationEventListener;

/**
 * This class is responsible for responding to Observation events and dispatching notifications to
 * the client.
 */
public class ObservationNotificationListener implements IObservationEventListener {

	public static int OBSERVATION_NOTIFICATION_ID = 1415;
	private Context mContext;
	private SharedPreferences mPreferences;

	/**
	 * Constructor.
	 *
	 * @param context An application context used for reading in preferences and dispatching
	 *                notifications.
	 */
	public ObservationNotificationListener(Context context) {
		mContext = context;
		mPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
	}

	@Override
	public void onObservationCreated(Collection<Observation> observations, Boolean sendNotifications) {

		if(sendNotifications != null && sendNotifications) {
			// are we configured to fire notifications?
			Boolean notificationsEnabled = mPreferences.getBoolean(mContext.getString(R.string.notificationsEnabledKey), mContext.getResources().getBoolean(R.bool.notificationsEnabledDefaultValue));

			// are any of the observations remote?  We don't want to fire on locally created
			// observations.
			Boolean remoteObservations = Boolean.FALSE;
			if (notificationsEnabled) {
				for (Observation obs : observations) {
					if (obs.getRemoteId() != null) {
						remoteObservations = true;
						break;
					}
				}
			}

			// Should a notification be presented to the user?
			if (notificationsEnabled && remoteObservations && (observations.size()) > 0) {

				// Build intent for notification content
				Intent viewIntent = new Intent(mContext, LandingActivity.class);
				//viewIntent.putExtra(EXTRA_EVENT_ID, eventId);
				PendingIntent viewPendingIntent =
						PendingIntent.getActivity(mContext, 0, viewIntent, 0);

				NotificationCompat.Builder notificationBuilder =
						new NotificationCompat.Builder(mContext, MageApplication.MAGE_NOTIFICATION_CHANNEL_ID)
								.setSmallIcon(R.drawable.ic_new_obs)
								.setContentTitle("New MAGE Observation(s)")
								.setContentText("Touch for details")
								.setVibrate(new long[]{0, 400, 75, 250, 75, 250})
								.setPriority(NotificationCompat.PRIORITY_MAX)
								.setAutoCancel(true)
								.setContentIntent(viewPendingIntent);

				// Get an instance of the NotificationManager service
				NotificationManagerCompat notificationManager = NotificationManagerCompat.from(mContext);

				// Build the notification and issues it with notification manager.
				notificationManager.notify(OBSERVATION_NOTIFICATION_ID, notificationBuilder.build());
			}
		}
	}

	@Override
	public void onObservationUpdated(Observation observation) {
		//do nothing
	}

	@Override
	public void onObservationDeleted(Observation observation) {
		//do nothing
	}

	@Override
	public void onError(Throwable throwable) {
		//do nothing
	}

}
