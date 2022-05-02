package mil.nga.giat.mage.observation.sync

import android.content.Context
import mil.nga.giat.mage.sdk.datastore.observation.Observation
import mil.nga.giat.mage.sdk.datastore.observation.ObservationHelper
import mil.nga.giat.mage.sdk.event.IObservationEventListener

class ObservationSyncListener(
   val context: Context,
   val sync : () -> Unit
): IObservationEventListener {

   init {
      ObservationHelper.getInstance(context).addListener(this)
      sync()
   }

   override fun onObservationCreated(
      observations: MutableCollection<Observation>,
      sendUserNotifcations: Boolean?
   ) {
      if (observations.any { it.isDirty }) {
         sync()
      }
   }

   override fun onObservationUpdated(observation: Observation) {
      if (observation.isDirty) {
         sync()
      }
   }

   override fun onObservationDeleted(observation: Observation?) {}
   override fun onError(error: Throwable?) {}
}