package mil.nga.giat.mage.network.observation

import android.util.Log
import com.google.gson.GsonBuilder
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import mil.nga.giat.mage.database.model.observation.Attachment
import mil.nga.giat.mage.database.model.observation.Observation
import mil.nga.giat.mage.database.model.observation.ObservationFavorite
import mil.nga.giat.mage.database.model.observation.ObservationForm
import mil.nga.giat.mage.database.model.observation.ObservationImportant
import mil.nga.giat.mage.database.model.observation.ObservationProperty
import mil.nga.giat.mage.database.model.observation.State
import mil.nga.giat.mage.network.attachment.AttachmentTypeAdapter
import mil.nga.giat.mage.network.geojson.GeometryTypeAdapterFactory
import mil.nga.giat.mage.network.gson.*
import mil.nga.giat.mage.network.gson.nextBooleanOrNull
import mil.nga.giat.mage.network.gson.nextDoubleOrNull
import mil.nga.giat.mage.network.gson.nextNumberOrNull
import mil.nga.giat.mage.network.gson.nextStringOrNull
import mil.nga.giat.mage.sdk.utils.ISO8601DateFormatFactory
import mil.nga.giat.mage.sdk.utils.toBytes
import mil.nga.sf.Geometry
import java.io.IOException
import java.text.ParseException
import java.util.*

class ObservationDeserializer {
   private val gson = GsonBuilder().registerTypeAdapterFactory(GeometryTypeAdapterFactory()).create()
   private val attachmentDeserializer = AttachmentTypeAdapter()

   fun read(reader: JsonReader): Observation {
      val observation =
         Observation()

      if (reader.peek() != JsonToken.BEGIN_OBJECT) {
         reader.skipValue()
         return observation
      }

      reader.beginObject()

      while(reader.hasNext()) {
         when(reader.nextName()) {
            "id" -> {
               observation.isDirty = false
               observation.remoteId = reader.nextString()
            }
            "userId" -> observation.userId = reader.nextStringOrNull()
            "deviceId" -> observation.deviceId = reader.nextStringOrNull()
            "lastModified" -> {
               try {
                  observation.lastModified = ISO8601DateFormatFactory.ISO8601().parse(reader.nextString())
               } catch (e: Exception) {
                  Log.e(LOG_NAME, "Error parsing observation date.", e)
               }
            }
            "url" -> observation.url = reader.nextString()
            "state" -> observation.state = readState(reader)
            "geometry" -> observation.geometry = gson.fromJson(reader, Geometry::class.java)
            "properties" -> readProperties(reader, observation)
            "attachments" -> observation.attachments = readAttachments(reader)
            "important" -> observation.important = readImportant(reader)
            "favoriteUserIds" -> observation.favorites = readFavoriteUsers(reader)
            else -> reader.skipValue()
         }
      }

      reader.endObject()

      return observation
   }

   @Throws(IOException::class)
   private fun readState(reader: JsonReader): State {
      var state = State.ACTIVE

      if (reader.peek() != JsonToken.BEGIN_OBJECT) {
         reader.skipValue()
         return state
      }

      reader.beginObject()

      while(reader.hasNext()) {
         when(reader.nextName()) {
            "name" -> {
               val stateString = reader.nextString()
               if (stateString != null) {
                  try {
                     state = State.valueOf(stateString.trim().uppercase())
                  } catch (e: Exception) {
                     Log.e(LOG_NAME, "Could not parse state: $stateString")
                  }
               }
            }
            else -> reader.skipValue()
         }
      }

      reader.endObject()

      return state
   }

   @Throws(IOException::class)
   private fun readProperties(reader: JsonReader, observation: Observation): List<ObservationProperty> {
      val properties = mutableListOf<ObservationProperty>()

      if (reader.peek() != JsonToken.BEGIN_OBJECT) {
         reader.skipValue()
         return properties
      }

      reader.beginObject()

      while(reader.hasNext()) {
         when(reader.nextName()) {
            "timestamp" -> {
               val timestamp = reader.nextString()
               try {
                  observation.timestamp = ISO8601DateFormatFactory.ISO8601().parse(timestamp)
               } catch (e: ParseException) {
                  Log.e(LOG_NAME, "Unable to parse date: " + timestamp + " for observation: " + observation.remoteId, e)
               }
            }
            "provider" -> observation.provider = reader.nextStringOrNull()
            "accuracy" -> {
               try {
                  observation.accuracy = reader.nextDoubleOrNull()?.toFloat()
               } catch (e: Exception) {
                  Log.e(LOG_NAME, "Error parsing observation accuracy", e)
               }
            }
            "forms" -> observation.forms = readForms(reader)
            else -> reader.skipValue()
         }
      }

      reader.endObject()

      return properties
   }

   @Throws(IOException::class)
   private fun readForms(reader: JsonReader): List<ObservationForm> {
      val forms = mutableListOf<ObservationForm>()

      if (reader.peek() != JsonToken.BEGIN_ARRAY) {
         reader.skipValue()
         return forms
      }

      reader.beginArray()

      while(reader.hasNext()) {
         forms.add(readForm(reader))
      }

      reader.endArray()

      return forms
   }

   @Throws(IOException::class)
   private fun readForm(reader: JsonReader): ObservationForm {
      val form = ObservationForm()
      val properties: MutableCollection<ObservationProperty> = ArrayList()

      if (reader.peek() != JsonToken.BEGIN_OBJECT) {
         reader.skipValue()
         return form
      }

      reader.beginObject()

      while(reader.hasNext()) {
         when(val key = reader.nextName()) {
            "id" -> form.remoteId = reader.nextString()
            "formId" -> form.formId = reader.nextLong()
            else -> {
               readProperty(key, reader)?.let {
                  properties.add(it)
               }
            }
         }
      }

      reader.endObject()

      form.properties = properties
      return form
   }

   private fun readProperty(key: String, reader: JsonReader): ObservationProperty? {
      var property: ObservationProperty? = null

      try {
         when(reader.peek()) {
            JsonToken.BEGIN_OBJECT -> {
               try {
                  val geometry: Geometry = gson.fromJson(reader, Geometry::class.java)
                  val geometryBytes = geometry.toBytes()
                  property = ObservationProperty(key, geometryBytes)
               } catch (_: Exception) {}
            }
            JsonToken.BEGIN_ARRAY -> {
               val stringArrayList = ArrayList<String>()
               val attachments = ArrayList<Attachment>()

               reader.beginArray()

               while(reader.hasNext()) {
                  if (reader.peek() == JsonToken.BEGIN_OBJECT) {
                     val attachment = attachmentDeserializer.read(reader)
                     attachments.add(attachment)
                  } else {
                     stringArrayList.add(reader.nextString())
                  }
               }

               if (stringArrayList.isNotEmpty()) {
                  property =
                     ObservationProperty(
                        key,
                        stringArrayList
                     )
               } else if (attachments.isNotEmpty()) {
                  property =
                     ObservationProperty(
                        key,
                        attachments
                     )
               }

               reader.endArray()
            }
            JsonToken.NUMBER -> {
               reader.nextNumberOrNull()?.let {
                  property =
                     ObservationProperty(
                        key,
                        it
                     )
               }
            }
            JsonToken.BOOLEAN -> {
               reader.nextBooleanOrNull()?.let {
                  property =
                     ObservationProperty(
                        key,
                        it
                     )
               }
            }
            JsonToken.STRING -> {
               reader.nextStringOrNull()?.let {
                  property =
                     ObservationProperty(
                        key,
                        it
                     )
               }
            }
            else -> reader.skipValue()
         }
      } catch (e: Exception) {
         Log.w(LOG_NAME, "Error parsing observation property, skipping...", e)
      }

      return property
   }

   @Throws(IOException::class)
   private fun readAttachments(reader: JsonReader): List<Attachment> {
      val attachments = mutableListOf<Attachment>()

      if (reader.peek() != JsonToken.BEGIN_ARRAY) {
         reader.skipValue()
         return attachments
      }

      reader.beginArray()

      while(reader.hasNext()) {
         attachments.add(attachmentDeserializer.read(reader))
      }

      reader.endArray()

      return attachments
   }

   @Throws(IOException::class)
   private fun readImportant(reader: JsonReader): ObservationImportant {
      val important =
         ObservationImportant()
      important.isImportant = true
      important.isDirty = false

      if (reader.peek() != JsonToken.BEGIN_OBJECT) {
         reader.skipValue()
         return important
      }

      reader.beginObject()

      while(reader.hasNext()) {
         when(reader.nextName()) {
            "userId" -> important.userId = reader.nextStringOrNull()
            "description" -> important.description = reader.nextStringOrNull()
            "timestamp" -> {
               try {
                  important.timestamp = ISO8601DateFormatFactory.ISO8601().parse(reader.nextString())
               } catch (e: Exception) {
                  Log.e(LOG_NAME, "Unable to parse observation important date", e)
               }
            }
            else -> reader.skipValue()
         }
      }

      reader.endObject()

      return important
   }

   @Throws(IOException::class)
   private fun readFavoriteUsers(reader: JsonReader): List<ObservationFavorite> {
      val favorites = mutableListOf<ObservationFavorite>()

      if (reader.peek() != JsonToken.BEGIN_ARRAY) {
         reader.skipValue()
         return favorites
      }

      reader.beginArray()

      while(reader.hasNext()) {
         if (reader.peek() == JsonToken.STRING) {
            val userId = reader.nextString()
            val favorite =
               ObservationFavorite(
                  userId,
                  true
               )
            favorite.isDirty = false
            favorites.add(favorite)
         } else reader.skipValue()
      }

      reader.endArray()

      return favorites
   }

   companion object {
      private val LOG_NAME = ObservationDeserializer::class.java.name
   }
}