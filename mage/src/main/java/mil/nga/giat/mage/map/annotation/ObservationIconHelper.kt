package mil.nga.giat.mage.map.annotation

import android.content.Context
import android.net.Uri
import mil.nga.giat.mage.data.repository.event.EventRepository
import mil.nga.giat.mage.database.model.event.Event
import mil.nga.giat.mage.database.model.event.Form
import mil.nga.giat.mage.database.model.feed.Feed
import mil.nga.giat.mage.database.model.geojson.StaticFeature
import mil.nga.giat.mage.database.model.observation.ObservationForm
import mil.nga.giat.mage.database.model.user.User
import mil.nga.giat.mage.network.Server
import java.io.File
import java.io.FileFilter
import java.util.Stack
import androidx.core.net.toUri

object ObservationIconHelper {
    private val fileFilter =
        FileFilter { path: File ->
            path.isFile && path.name.startsWith("icon.")
        }

    fun getObservationIconUriFromStaticFeatures(feature: StaticFeature): Uri? {
        val iconUri = feature.localPath?.let { path ->
            val file = File(path)
            if (file.exists()) {
                Uri.fromFile(file)
            } else null
        }

        return iconUri
    }

    fun getObservationIconUriFromObservation(
        event: Event?,
        formDefinition: Form?,
        observationForm: ObservationForm?,
        context: Context
    ): Uri? {
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

        val file = recurseIconPath(iconProperties, path, 0)
        return Uri.fromFile(file)
    }

    fun getObservationIconUriFromProperties(eventId: String, formId: Long?, primary: String?, secondary: String?, context: Context): Uri? {
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

        val file = recurseIconPath(iconProperties, path, 0)
        return Uri.fromFile(file)
    }

    fun getObservationIconUriFromUser(user: User): Uri? {
        val iconPath = if (user.iconPath != null) {
            File(user.iconPath)
        } else null

        val iconUri = iconPath?.let { Uri.fromFile(it) }
        return iconUri
    }

    fun getObservationIconUriFromFeed(feed: Feed, context: Context): Uri? {
        val iconUri = feed.mapStyle?.iconStyle?.id?.let {
            "${Server(context).baseUrl}/api/icons/${it}/content".toUri()
        }

        return iconUri
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