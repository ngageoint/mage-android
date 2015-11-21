package mil.nga.giat.mage.sdk.push;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import mil.nga.giat.mage.sdk.datastore.observation.Attachment;
import mil.nga.giat.mage.sdk.datastore.observation.AttachmentHelper;
import mil.nga.giat.mage.sdk.event.IAttachmentEventListener;

/**
 * Service to handle pushing observation attachments
 *
 * @author newmanw
 *
 */
public class AttachmentPushService implements IAttachmentEventListener {

    private static final String LOG_NAME = AttachmentPushService.class.getName();

    private static final int ATTACHMENT_PUSH_INTERVAL = 60000;

    private Context mContext;
    private AlarmManager alarm;
    private Intent attachmentPushIntent;

    /**
     * Constructor.
     *
     * @param context An application context used for reading in preferences and dispatching
     *                notifications.
     */
    public AttachmentPushService(Context context) {
        mContext = context;
    }

    public void start() {
        AttachmentHelper.getInstance(mContext).addListener(this);

        alarm = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);

        if (attachmentPushIntent == null) {
            attachmentPushIntent = new Intent(mContext, AttachmentPushAlarmReceiver.class);
            final PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, AttachmentPushAlarmReceiver.REQUEST_CODE, attachmentPushIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            long firstMillis = System.currentTimeMillis();
            alarm.setInexactRepeating(AlarmManager.RTC_WAKEUP, firstMillis, ATTACHMENT_PUSH_INTERVAL, pendingIntent);
        }
    }

    public void stop() {
        AttachmentHelper.getInstance(mContext).removeListener(this);

        Intent intent = new Intent(mContext, AttachmentPushAlarmReceiver.class);
        final PendingIntent pIntent = PendingIntent.getBroadcast(mContext, AttachmentPushAlarmReceiver.REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarm.cancel(pIntent);

        if(attachmentPushIntent != null) {
            mContext.stopService(attachmentPushIntent);
            attachmentPushIntent = null;
        }
    }

    @Override
    public void onAttachmentUploadable(Attachment attachment) {
        Log.i(LOG_NAME, "Attachment uploadable event received");

        Intent attachmentIntent = new Intent(mContext, AttachmentPushIntentService.class);
        attachmentIntent.putExtra(AttachmentPushIntentService.ATTACHMENT_ID, attachment.getId());
        mContext.startService(attachmentIntent);
    }

    @Override
    public void onAttachmentCreated(Attachment attachment) {

    }

    @Override
    public void onAttachmentUpdated(Attachment attachment) {

    }

    @Override
    public void onAttachmentDeleted(Attachment attachment) {

    }

    @Override
    public void onError(Throwable throwable) {

    }
}
