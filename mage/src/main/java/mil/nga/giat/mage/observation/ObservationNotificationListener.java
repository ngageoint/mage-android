package mil.nga.giat.mage.observation;

import mil.nga.giat.mage.LandingActivity;
import mil.nga.giat.mage.R;
import mil.nga.giat.mage.sdk.datastore.observation.Observation;
import mil.nga.giat.mage.sdk.event.IObservationEventListener;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationManagerCompat;

import java.util.Collection;

import static android.support.v4.app.NotificationCompat.Builder;

/**
 * This class is responsible for responding to Observation events and dispatching notifications to
 * the client.
 */
public class ObservationNotificationListener implements IObservationEventListener {

	private Context mContext;
    private SharedPreferences mPreferences;

    public static int OBSERVATION_NOTIFICATION_ID = 1415;


    /**
     * Constructor.
     * @param context An application context used for reading in preferences and dispatching
     *                notifications.
     */
    public ObservationNotificationListener(Context context) {
        mContext = context;
        mPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
    }

    @Override
    public void onObservationCreated(Collection<Observation> observations) {

        // are we configured to fire notifications?
        Boolean fireNotification =
                mPreferences.getBoolean(mContext.getString(R.string.notificationsEnabledKey),
                                        Boolean.FALSE);

        // are any of the observations remote?  We don't want to fire on locally created
        // observations.
        Boolean remoteObservations = Boolean.FALSE;
        if(fireNotification) {
            for(Observation obs : observations) {
                if(obs.getRemoteId() != null) {
                    remoteObservations = Boolean.TRUE;
                    break;
                }
            }
        }

        // to fire, or not to fire...THAT is the question.
        if(fireNotification && remoteObservations && observations.size() > 0 ) {

            // Build intent for notification content
            Intent viewIntent = new Intent(mContext, LandingActivity.class);
            //viewIntent.putExtra(EXTRA_EVENT_ID, eventId);
            PendingIntent viewPendingIntent =
                    PendingIntent.getActivity(mContext, 0, viewIntent, 0);

            Builder notificationBuilder =
                    new Builder(mContext)
                            .setSmallIcon(R.drawable.ic_new_obs)
                            .setContentTitle("New MAGE Observation(s)")
                            .setContentText("Log into application for more details.")
                            .setContentIntent(viewPendingIntent);

            //TODO: Might not need this.  Pending intent seems to work...
            //notificationBuilder.addAction(R.drawable.ic_launcher,
            //        mContext.getText(R.string.app_name),
            //        viewPendingIntent);

            // Get an instance of the NotificationManager service
            NotificationManagerCompat notificationManager =
                    NotificationManagerCompat.from(mContext);

            // Build the notification and issues it with notification manager.
            notificationManager.notify(OBSERVATION_NOTIFICATION_ID, notificationBuilder.build());

            // pulse the vibrator
            Vibrator vibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
            vibrator.vibrate(50);

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
