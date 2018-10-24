package mil.nga.giat.mage.observation.sync

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.work.*
import dagger.android.AndroidInjection
import mil.nga.giat.mage.sdk.datastore.observation.Observation
import mil.nga.giat.mage.sdk.datastore.observation.ObservationHelper
import mil.nga.giat.mage.sdk.event.IObservationEventListener
import java.util.*
import javax.inject.Inject

class ObservationPushService : Service(), IObservationEventListener {

    @Inject
    lateinit var context: Context

    companion object {
        private val OBSERVATION_SYNC_WORK = "mil.nga.mage.OBSERVATION_SYNC_WORK"

        private fun workRequest(): OneTimeWorkRequest {
            val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

            return OneTimeWorkRequestBuilder<ObservationSyncWorker>()
                    .setConstraints(constraints)
                    .build()
        }

        fun beginWork(): UUID {
            val request = workRequest()
            WorkManager.getInstance().beginUniqueWork(OBSERVATION_SYNC_WORK, ExistingWorkPolicy.APPEND, request).enqueue()
            return request.id
        }
    }

    override fun onCreate() {
        super.onCreate()

        AndroidInjection.inject(this)

        ObservationHelper.getInstance(context).addListener(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        // Try and sync anything that may not have already been sync'ed
        // This could happen if observations, important and/or favorties were created when the user
        // did not have a token
        if (shouldSync()) {
            beginWork()
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()

        ObservationHelper.getInstance(context).removeListener(this)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onObservationCreated(observations: MutableCollection<Observation>, sendUserNotifcations: Boolean) {
        if (observations.any({it.isDirty})) {
            beginWork()
        }
    }

    override fun onObservationUpdated(observation: Observation) {
        if (observation.isDirty ||
                observation.important?.isDirty == true ||
                observation.favorites.any({it.isDirty})) {
            beginWork()
        }
    }

    override fun onObservationDeleted(observation: Observation) {
    }

    override fun onError(error: Throwable?) {
    }

    private fun shouldSync(): Boolean {
        val observationHelper = ObservationHelper.getInstance(context)
        return observationHelper.dirty.isNotEmpty() ||
                observationHelper.dirtyImportant.isNotEmpty() ||
                observationHelper.dirtyFavorites.isNotEmpty()
    }
}
