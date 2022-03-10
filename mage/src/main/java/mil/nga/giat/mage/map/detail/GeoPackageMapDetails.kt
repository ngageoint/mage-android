package mil.nga.giat.mage.map.detail

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import mil.nga.giat.mage.map.GeoPackageAttribute
import mil.nga.giat.mage.map.GeoPackageFeatureMapState
import mil.nga.giat.mage.map.GeoPackageMediaProperty
import mil.nga.giat.mage.map.GeoPackageProperty
import mil.nga.giat.mage.utils.DateFormatFactory
import mil.nga.sf.Geometry
import java.util.*

sealed class GeoPackageFeatureAction {
   class Directions(val geometry: Geometry, val icon: Any?): GeoPackageFeatureAction()
   class Location(val geometry: Geometry): GeoPackageFeatureAction()
   class Media(val geoPackage: String, val mediaTable: String, val mediaId: Long): GeoPackageFeatureAction()
}

@Composable
fun GeoPackageFeatureDetails(
   featureMapState: GeoPackageFeatureMapState?,
   onAction: ((Any) -> Unit)? = null
) {
   if (featureMapState != null) {
      FeatureDetails(
         featureMapState,
         onAction = { action ->
            when (action) {
               is FeatureAction.Directions<*> -> {
                  onAction?.invoke(GeoPackageFeatureAction.Directions(action.geometry, action.image))

               }
               is FeatureAction.Location -> {
                  onAction?.invoke(GeoPackageFeatureAction.Location(action.geometry))
               }
            }
         },
         details = {
            Column {
               if (featureMapState.properties.isNotEmpty()) {
                  Divider(
                     color = MaterialTheme.colors.onSurface.copy(alpha = 0.08f),
                     modifier = Modifier.height(8.dp)
                  )

                  CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                     Text(
                        text = "DETAILS",
                        style = MaterialTheme.typography.caption,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(8.dp)
                     )
                  }

                  GeoPackageProperties(featureMapState.properties) { property ->
                     onAction?.invoke(GeoPackageFeatureAction.Media(featureMapState.geoPackage, property.mediaTable, property.mediaId))
                  }
               }

               if (featureMapState.attributes.isNotEmpty()) {
                  Divider(
                     color = MaterialTheme.colors.onSurface.copy(alpha = 0.08f),
                     modifier = Modifier.height(8.dp)
                  )

                  CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                     Text(
                        text = "ATTRIBUTES",
                        style = MaterialTheme.typography.caption,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(8.dp)
                     )
                  }

                  GeoPackageAttributes(featureMapState.attributes) { property ->
                     onAction?.invoke(GeoPackageFeatureAction.Media(featureMapState.geoPackage, property.mediaTable, property.mediaId))
                  }
               }
            }
         }
      )
   }
}

@Composable
fun GeoPackageProperties(
   properties: List<GeoPackageProperty>,
   onClick: ((GeoPackageMediaProperty) -> Unit)? = null
) {
   val context = LocalContext.current

   Column(Modifier.padding(horizontal = 16.dp)) {
      properties
         .sortedBy { property -> property.key }
         .forEach { property ->
            Column(Modifier.padding(bottom = 16.dp)) {
               CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                  Text(
                     modifier = Modifier.padding(bottom = 4.dp),
                     text = property.key,
                     style = MaterialTheme.typography.overline,
                     fontWeight = FontWeight.Bold
                  )
               }

               when {
                  property is GeoPackageMediaProperty -> {
                     GeoPackageMedia(property) {
                        onClick?.invoke(property)
                     }
                  }
                  property.value is Boolean -> {
                     val text = if (property.value.toString().toInt() == 1) "true" else "false"
                     GeoPackageAttributeText(text)
                  }
                  property.value is Date -> {
                     val dateFormat = DateFormatFactory.format("yyyy-MM-dd HH:mm zz", Locale.getDefault(), context)
                     val text = (property.value as? Date)?.let { dateFormat.format(it) } ?: property.value.toString()
                     GeoPackageAttributeText(text)
                  }
                  property.value is Number -> {
                     GeoPackageAttributeText(property.value.toString())
                  }
                  else -> {
                     GeoPackageAttributeText(property.value.toString())
                  }
               }
            }
         }
   }
}

@Composable
private fun GeoPackageAttributes(
   attributes: List<GeoPackageAttribute>,
   onClick: ((GeoPackageMediaProperty) -> Unit)? = null
) {
   attributes.forEach { attribute ->
      Column {
         GeoPackageProperties(properties = attribute.properties) { onClick?.invoke(it) }
      }
   }
}

@Composable
private fun GeoPackageAttributeText(value: String) {
   CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.high) {
      Text(
         text = value,
         style = MaterialTheme.typography.subtitle1
      )
   }
}

@Composable
private fun GeoPackageMedia(
   property: GeoPackageMediaProperty,
   onClick: (() -> Unit)? = null
) {
   when {
      property.contentType.contains("image/") -> {
         val bitmap = BitmapFactory.decodeByteArray(property.value, 0, property.value.size)
         GeoPackageImage(bitmap, onClick)
      }
      property.contentType.contains("video/") -> {
         GeoPackageMediaIcon(Icons.Default.PlayArrow, onClick)
      }
      property.contentType.contains("audio/") -> {
         GeoPackageMediaIcon(Icons.Default.VolumeUp, onClick)
      }
      else -> {
         GeoPackageMediaIcon(Icons.Default.AttachFile, onClick)
      }
   }
}

@Composable
private fun GeoPackageImage(
   bitmap: Bitmap,
   onClick: (() -> Unit)? = null
) {
   Box(
      Modifier
         .fillMaxWidth()
         .height(200.dp)
         .clip(MaterialTheme.shapes.medium)
         .background(MaterialTheme.colors.onSurface.copy(alpha = 0.08f))
         .background(Color(0x19FFFFFF))
         .clickable { onClick?.invoke() }
   ) {
      Image(
         bitmap = bitmap.asImageBitmap(),
         contentDescription = "Image from GeoPackage",
         modifier = Modifier.fillMaxSize()
      )
   }
}

@Composable
private fun GeoPackageMediaIcon(
   icon: ImageVector,
   onClick: (() -> Unit)? = null
) {
   Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier
         .fillMaxWidth()
         .height(200.dp)
         .clip(MaterialTheme.shapes.medium)
         .background(MaterialTheme.colors.onSurface.copy(alpha = 0.08f))
         .clickable { onClick?.invoke() }
   ) {

      Box(
         contentAlignment = Alignment.Center,
         modifier = Modifier
            .height(144.dp)
            .width(144.dp)
            .clip(CircleShape)
            .background(Color(0x54000000))
      ) {
         Icon(
            imageVector = icon,
            contentDescription = "Media Icon",
            tint = Color(0xDEFFFFFF),
            modifier = Modifier
               .height(84.dp)
               .width(84.dp)
         )
      }
   }
}