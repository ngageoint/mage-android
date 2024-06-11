package mil.nga.giat.mage.map.annotation

import android.content.Context
import android.net.Uri
import mil.nga.giat.mage.data.repository.event.EventRepository
import mil.nga.giat.mage.database.model.event.Event
import mil.nga.giat.mage.database.model.event.Form
import mil.nga.giat.mage.database.model.observation.ObservationForm
import java.io.File
import java.io.FileFilter
import java.util.*

open class IconStyle(
   val uri: Uri? = null
): AnnotationStyle() {
   companion object {
      private val fileFilter =
         FileFilter { path: File ->
            path.isFile && path.name.startsWith("icon.")
         }

      fun recurseIconPath(iconProperties: Stack<String>, file: File, index: Int): File? {
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

class ObservationIconStyle: IconStyle() {
   companion object {
      fun fromObservation(
         event: Event?,
         formDefinition: Form?,
         observationForm: ObservationForm?,
         context: Context
      ): IconStyle {
         val iconUri = observationIcon(
            event,
            formDefinition,
            observationForm,
            context
         )?.let { file ->
            Uri.fromFile(file)
         }

         return IconStyle(iconUri)
      }

      fun fromObservationPropertyValues(
         eventId: String,
         formId: Long?,
         primaryFieldText: String?,
         secondaryFieldText: String?,
         context: Context
      ): File? {

         val path = File(File(File(context.filesDir.absolutePath + EventRepository.OBSERVATION_ICON_PATH), eventId), "icons")
         val iconProperties = Stack<String>()

         secondaryFieldText?.let {
            iconProperties.add(it)
         }

         primaryFieldText?.let {
            iconProperties.add(it)
         }

         formId?.let { iconProperties.add(it.toString()) }

         return recurseIconPath(iconProperties, path, 0)
      }

      fun fromObservationProperties(eventId: String, formId: Long?, primary: String?, secondary: String?, context: Context): IconStyle {
         val iconUri = observationIcon(eventId, formId, primary, secondary, context)
            ?.let { file ->
               Uri.fromFile(file)
            }

         return IconStyle(iconUri)
      }

      private fun observationIcon(
         event: Event?,
         formDefinition: Form?,
         observationForm: ObservationForm?,
         context: Context
      ): File? {
         event ?: return null

         val path = File(File(File(context.filesDir.absolutePath + EventRepository.OBSERVATION_ICON_PATH), event.remoteId), "icons")
         val iconProperties = Stack<String>()

         formDefinition?.secondaryMapField?.let { field ->
            observationForm?.properties?.find { it.key ==  field }?.value?.toString()?.let {
               iconProperties.add(it)
            }
         }

         formDefinition?.primaryMapField?.let { field ->
            observationForm?.properties?.find { it.key ==  field }?.value?.toString()?.let {
               iconProperties.add(it)
            }
         }

         observationForm?.formId?.let { iconProperties.add(it.toString()) }

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
   }
}
