package mil.nga.giat.mage.observation.sync

import android.content.Context
import android.util.Log
import androidx.preference.PreferenceManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import mil.nga.giat.mage.form.FieldType
import mil.nga.giat.mage.form.Form
import mil.nga.giat.mage.form.field.Media
import mil.nga.giat.mage.sdk.R
import mil.nga.giat.mage.sdk.datastore.observation.*
import mil.nga.giat.mage.sdk.exceptions.ObservationException
import mil.nga.giat.mage.sdk.http.HttpClientManager
import mil.nga.giat.mage.sdk.http.converter.ObservationConverterFactory
import mil.nga.giat.mage.sdk.http.converter.ObservationImportantConverterFactory
import mil.nga.giat.mage.sdk.http.resource.ObservationResource
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.net.HttpURLConnection

class ObservationSyncWorker(var context: Context, params: WorkerParameters) : Worker(context, params) {

    private fun Result.withFlag(flag: Int): Int {
        return when(this) {
            is Result.Failure -> RESULT_FAILURE_FLAG or flag
            is Result.Retry -> RESULT_RETRY_FLAG or flag
            else -> RESULT_SUCCESS_FLAG or flag
        }
    }

    private fun Int.containsFlag(flag: Int): Boolean {
        return (this or flag) == this
    }

    private fun Int.withFlag(flag: Int): Int {
        return this or flag
    }

    companion object {
        private val LOG_NAME = ObservationSyncWorker::class.java.simpleName

        private const val RESULT_SUCCESS_FLAG = 0
        private const val RESULT_FAILURE_FLAG = 1
        private const val RESULT_RETRY_FLAG = 2
    }

    override fun doWork(): Result {
        var result = RESULT_SUCCESS_FLAG

        try {
            result = syncObservations().withFlag(result)
            result = syncObservationImportant().withFlag(result)
            result = syncObservationFavorites().withFlag(result)
        } catch (e: Exception) {
            Log.e(LOG_NAME, "Error trying to sync observations with server", e)
        }

        return if (result.containsFlag(RESULT_RETRY_FLAG)) Result.retry() else Result.success()
    }

    private fun syncObservations(): Int {
        var result = RESULT_SUCCESS_FLAG

        val observationHelper = ObservationHelper.getInstance(applicationContext)
        for (observation in observationHelper.dirty) {
            result = syncObservation(observation).withFlag(result)
        }

        return result
    }

    private fun syncObservation(observation: Observation): Int {
        var result = RESULT_SUCCESS_FLAG

        try {
            result = if (observation.state == State.ARCHIVE) {
                archive(observation).withFlag(result)
            } else {
                save(observation).withFlag(result)
            }
        } catch(e: Exception) {
            Log.e(LOG_NAME, "Failed to sync observation with server", e)
        }

        return result
    }

    private fun syncObservationImportant(): Int {
        var result = RESULT_SUCCESS_FLAG

        for (observation in ObservationHelper.getInstance(context).dirtyImportant) {
            result = updateImportant(observation).withFlag(result)
        }

        return result
    }

    private fun syncObservationFavorites(): Int {
        var result = RESULT_SUCCESS_FLAG

        for (favorite in ObservationHelper.getInstance(context).dirtyFavorites) {
            result = updateFavorite(favorite).withFlag(result)
        }

        return result
    }

    private fun save(observation: Observation): Result {
        return if (observation.remoteId.isNullOrEmpty()) {
            create(observation)
        } else {
            update(observation)
        }
    }

    private fun create(observation: Observation): Result {
        val baseUrl = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.serverURLKey), context.getString(R.string.serverURLDefaultValue))
        val retrofit = Retrofit.Builder()
                .baseUrl(baseUrl!!)
                .addConverterFactory(ObservationConverterFactory.create(observation.event))
                .client(HttpClientManager.getInstance().httpClient())
                .build()

        val service = retrofit.create(ObservationResource.ObservationService::class.java)
        val response = service.createObservationId(observation.event.remoteId).execute()

        if (response.isSuccessful) {
            val returnedObservation = response.body()
            observation.remoteId = returnedObservation?.remoteId
            observation.url = returnedObservation?.url

            // Got the observation id from the server, lets send the observation
            val result = update(observation)
            if (result !is Result.Success) {
                return if (response.code() == HttpURLConnection.HTTP_UNAUTHORIZED) Result.failure() else Result.retry()
            }

            return result
        } else {
            Log.e(LOG_NAME, "Bad request.")

            val observationError = ObservationError()
            observationError.statusCode = response.code()
            observationError.description = response.message()

            response.errorBody()?.string()?.let {
                Log.e(LOG_NAME, it)
                val error = JsonParser().parse(it).asJsonObject
                observationError.message = error.get("message")?.asString
            }

            ObservationHelper.getInstance(context).update(observation)

            return if (response.code() == HttpURLConnection.HTTP_UNAUTHORIZED) Result.failure() else Result.retry()
        }
    }

    private fun  update(observation: Observation): Result {
        val baseUrl = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.serverURLKey), context.getString(R.string.serverURLDefaultValue))
        val retrofit = Retrofit.Builder()
                .baseUrl(baseUrl!!)
                .addConverterFactory(ObservationConverterFactory.create(observation.event))
                .client(HttpClientManager.getInstance().httpClient())
                .build()

        val service = retrofit.create(ObservationResource.ObservationService::class.java)
        val response = service.updateObservation(observation.event.remoteId, observation.remoteId, observation).execute()

        if (response.isSuccessful) {
            val returnedObservation = response.body()
            returnedObservation?.isDirty = false
            returnedObservation?.id = observation.id

            // Mark new attachments as dirty and set local path for upload
            for (observationForm in observation.forms) {
                val formDefinition = Form.fromJson(observation.event.formMap[observationForm.formId])
                for (observationProperty in observationForm.properties) {
                    val fieldDefinition = formDefinition?.fields?.find { it.name == observationProperty.key }
                    if (fieldDefinition?.type == FieldType.ATTACHMENT) {
                        for (attachment in observationProperty.value as List<Attachment>) {
                            if (attachment.action == Media.ATTACHMENT_ADD_ACTION) {
                                val returnedAttachment = returnedObservation?.attachments?.find { returnedAttachment ->
                                    attachment.url == null
                                        && attachment.name == returnedAttachment.name
                                        && attachment.fieldName == returnedAttachment.fieldName
                                        && attachment.contentType == returnedAttachment.contentType
                                }

                                if (returnedAttachment != null) {
                                    returnedAttachment.localPath = attachment.localPath
                                    returnedAttachment.isDirty = true
                                }
                            }
                        }
                    }
                }
            }

            ObservationHelper.getInstance(context).update(returnedObservation)

            return Result.success()
        } else {
            Log.e(LOG_NAME, "Bad request.")

            val observationError = ObservationError()
            observationError.statusCode = response.code()
            observationError.description = response.message()

            response.errorBody()?.string()?.let {
                Log.e(LOG_NAME, it)
                val error = JsonParser().parse(it).asJsonObject
                observationError.message = error.get("message")?.asString
            }

            observation.error = observationError
            ObservationHelper.getInstance(context).update(observation)

            return if (response.code() == HttpURLConnection.HTTP_UNAUTHORIZED) Result.failure() else Result.retry()
        }
    }

    private fun archive(observation: Observation): Result {
        val observationHelper = ObservationHelper.getInstance(context)

        val baseUrl = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.serverURLKey), context.getString(R.string.serverURLDefaultValue))
        val retrofit = Retrofit.Builder()
                .baseUrl(baseUrl!!)
                .addConverterFactory(GsonConverterFactory.create())
                .client(HttpClientManager.getInstance().httpClient())
                .build()

        val state = JsonObject()
        state.addProperty("name", "archive")

        val service = retrofit.create(ObservationResource.ObservationService::class.java)

        try {
            val response = service.archiveObservation(observation.event.remoteId, observation.remoteId, state).execute()

            if (response.isSuccessful) {
                observationHelper.delete(observation)
                return Result.success()
            } else if(response.code() == HttpURLConnection.HTTP_NOT_FOUND) {
                observationHelper.delete(observation)
                return Result.success()
            } else if (response.code() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                return Result.failure()
            } else {
                Log.e(LOG_NAME, "Bad request.")

                val observationError = ObservationError()
                observationError.statusCode = response.code()
                observationError.description = response.message()

                response.errorBody()?.string()?.let {
                    Log.e(LOG_NAME, it)
                    val error = JsonParser().parse(it).asJsonObject
                    observationError.message = error.get("message")?.asString
                }

                observationHelper.update(observation)
                return Result.retry()
            }
        } catch (e: IOException) {
            Log.e(LOG_NAME, "Failure archiving observation.", e)

            val observationError = ObservationError()
            observationError.message = "The Internet connection appears to be offline."
            observation.error = observationError
            try {
                observationHelper.update(observation)
            } catch (oe: ObservationException) {
                Log.e(LOG_NAME, "Problem archiving observation error", oe)
            }

            return Result.retry()
        }
    }

    private fun updateImportant(observation: Observation): Result {
        val observationHelper = ObservationHelper.getInstance(context)

        try {
            val baseUrl = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.serverURLKey), context.getString(R.string.serverURLDefaultValue))
            val retrofit = Retrofit.Builder()
                    .baseUrl(baseUrl!!)
                    .addConverterFactory(ObservationImportantConverterFactory.create(observation.event))
                    .client(HttpClientManager.getInstance().httpClient())
                    .build()

            val service = retrofit.create(ObservationResource.ObservationService::class.java)

            val response = if (observation.important?.isImportant == true) {
                val jsonImportant = JsonObject()
                jsonImportant.addProperty("description", observation.important?.description)
                service.addImportant(observation.event.remoteId, observation.remoteId, jsonImportant).execute()
            } else {
                service.removeImportant(observation.event.remoteId, observation.remoteId).execute()
            }

            if (response.isSuccessful) {
                val returnedObservation = response.body()
                observation.lastModified = returnedObservation?.lastModified
                observationHelper.updateImportant(observation)

                return Result.success()
            } else {
                Log.e(LOG_NAME, "Bad request.")
                response.errorBody()?.string()?.let {
                    Log.e(LOG_NAME, it)
                }

                return if (response.code() == HttpURLConnection.HTTP_UNAUTHORIZED) Result.failure() else Result.retry()
            }
        } catch (e: IOException) {
            Log.e(LOG_NAME, "Failure toogling observation important.", e)
            return Result.retry()
        }
    }

    private fun updateFavorite(favorite: ObservationFavorite): Result {
        val observationHelper = ObservationHelper.getInstance(context)
        val observation = favorite.observation

        try {
            val baseUrl = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.serverURLKey), context.getString(R.string.serverURLDefaultValue))
            val retrofit = Retrofit.Builder()
                    .baseUrl(baseUrl!!)
                    .addConverterFactory(ObservationConverterFactory.create(observation.event))
                    .client(HttpClientManager.getInstance().httpClient())
                    .build()

            val service = retrofit.create(ObservationResource.ObservationService::class.java)

            val response: Response<Observation>
            if (favorite.isFavorite()) {
                response = service.favoriteObservation(observation.event.remoteId, observation.remoteId).execute()
            } else {
                response = service.unfavoriteObservation(observation.event.remoteId, observation.remoteId).execute()
            }

            if (response.isSuccessful) {
                val updatedObservation = response.body()
                observation.lastModified = updatedObservation?.lastModified
                observationHelper.updateFavorite(favorite)

                return Result.success()
            } else {
                Log.e(LOG_NAME, "Bad request.")
                response.errorBody()?.string()?.let {
                    Log.e(LOG_NAME, it)
                }

                return if (response.code() == HttpURLConnection.HTTP_UNAUTHORIZED) Result.failure() else Result.retry()
            }
        } catch (e: IOException) {
            Log.e(LOG_NAME, "Failure toogling observation favorite.", e)
            return Result.retry()
        }
    }
}