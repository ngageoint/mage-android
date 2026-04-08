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

   override fun onObservationsCreated(
      observations: MutableCollection<Observation>,
      sendUserNotifcations: Boolean?
   ) {
      if (observations.any { it.isDirty }) {
         sync()
      }
   }

   override fun onObservationsUpdated(observations: Collection<Observation>) {
      if (observations.any { it.isDirty || it.important?.isDirty == true || it.favorites.any { it.isDirty } }) {
         sync()
      }
   }

   override fun onObservationsDeleted() {}
   override fun onError(error: Throwable?) {}
}