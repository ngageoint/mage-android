package mil.nga.giat.mage.observation.sync

import mil.nga.giat.mage.database.model.observation.Observation
import mil.nga.giat.mage.data.datasource.observation.ObservationLocalDataSource
import mil.nga.giat.mage.sdk.event.IObservationEventListener

class ObservationSyncListener(
   observationLocalDataSource: ObservationLocalDataSource,
   val sync : () -> Unit
): IObservationEventListener {

   init {
      observationLocalDataSource.addListener(this)
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
      if (observation.isDirty ||
         observation.important?.isDirty == true ||
         observation.favorites.any{ it.isDirty }) {
         sync()
      }
   }

   override fun onObservationDeleted(observation: Observation?) {}
   override fun onError(error: Throwable?) {}
}