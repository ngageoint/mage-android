package mil.nga.giat.mage.map.annotation

import android.content.Context
import android.net.Uri
import mil.nga.giat.mage.data.event.EventRepository
import mil.nga.giat.mage.sdk.datastore.observation.Observation
import mil.nga.giat.mage.sdk.datastore.staticfeature.StaticFeature
import mil.nga.giat.mage.sdk.datastore.user.EventHelper
import java.io.File
import java.io.FileFilter
import java.util.*

class IconStyle(
   val uri: Uri? = null
): AnnotationStyle() {

   companion object {
      fun fromObservation(observation: Observation, context: Context): IconStyle {
         val iconFile = observationIcon(observation, context)
         return IconStyle(Uri.fromFile(iconFile))
      }

      fun fromObservationProperties(eventId: String, formId: Long?, primary: String?, secondary: String?, context: Context): IconStyle {
         val iconFile = observationIcon(eventId, formId, primary, secondary, context)
         return IconStyle(Uri.fromFile(iconFile))
      }

      fun fromStaticFeature(feature: StaticFeature): IconStyle {
         val iconUri = feature.localPath?.let { path ->
            val file = File(path)
            if (file.exists()) {
               Uri.fromFile(file)
            } else null
         }

         return IconStyle(iconUri)
      }

      private fun observationIcon(observation: Observation, context: Context): File? {
         val path = File(File(File(context.filesDir.absolutePath + EventRepository.OBSERVATION_ICON_PATH), observation.event.remoteId), "icons")
         val iconProperties = Stack<String>()

         observation.forms.firstOrNull()?.let { observationForm ->
            val form = EventHelper.getInstance(context).getForm(observationForm.formId)

            form.secondaryMapField?.let { field ->
               observationForm.properties.find { it.key ==  field }?.value?.toString()?.let {
                  iconProperties.add(it)
               }
            }

            form.primaryMapField?.let { field ->
               observationForm.properties.find { it.key ==  field }?.value?.toString()?.let {
                  iconProperties.add(it)
               }
            }

            iconProperties.add(observationForm.formId.toString())
         }

         return recurseIconPath(iconProperties, path, 0)
      }

      private fun observationIcon(eventId: String, formId: Long?, primary: String?, secondary: String?, context: Context): File? {
         val path = File(File(File(context.filesDir.absolutePath + EventRepository.OBSERVATION_ICON_PATH), eventId), "icons")
         val iconProperties = Stack<String>()

         if (formId != null) {
            if (secondary != null) {
               iconProperties.add(secondary)
            }

            if (primary != null) {
               iconProperties.add(primary)
            }

            iconProperties.add(formId.toString())
         }

         return recurseIconPath(iconProperties, path, 0)
      }

      private val fileFilter =
         FileFilter { path: File ->
            path.isFile && path.name.startsWith("icon.")
         }

      private fun recurseIconPath(iconProperties: Stack<String>, file: File, index: Int): File? {
         var path: File? = file
         var i = index
         if (iconProperties.size > 0) {
            val property = iconProperties.pop()
            if (property != null && path?.exists() == true) {
               if (property.trim().isNotEmpty() && File(path, property).exists()) {
                  return recurseIconPath(iconProperties, File(path, property), i + 1)
               }
            }
         }

         while (path?.listFiles(fileFilter) != null && path.listFiles(fileFilter)?.size == 0 && i >= 0) {
            path = path.parentFile
            i--
         }

         if (path == null || !path.exists()) return null

         val files = path.listFiles(fileFilter)
         return if (files?.isNotEmpty() == true) {
            files[0]
         } else null
      }
   }
}