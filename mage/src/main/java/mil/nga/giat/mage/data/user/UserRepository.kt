package mil.nga.giat.mage.data.user

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mil.nga.giat.mage.network.api.UserService
import mil.nga.giat.mage.sdk.datastore.user.Event
import mil.nga.giat.mage.sdk.datastore.user.User
import mil.nga.giat.mage.sdk.datastore.user.UserHelper
import mil.nga.giat.mage.sdk.utils.MediaUtility
import okhttp3.ResponseBody
import java.io.File
import javax.inject.Inject

class UserRepository @Inject constructor(
   @ApplicationContext private val context: Context,
   private val userService: UserService
) {
   private val userHelper = UserHelper.getInstance(context)

   suspend fun syncIcons(event: Event) = withContext(Dispatchers.IO) {
      val users = userHelper.getUsersInEvent(event)
      for (user in users) {
         try {
            syncIcon(user)
         } catch (e: Exception) {
            Log.e(LOG_NAME, "Error syncing user icon", e)
         }
      }
   }

   private suspend fun syncIcon(user: User) {
      val path = "${MediaUtility.getUserIconDirectory(context)}/${user.id}.png"
      val file = File(path)
      if (file.exists()) {
         file.delete()
      }

      val response = userService.getIcon(user.remoteId)
      val body = response.body()
      if (response.isSuccessful && body != null) {
         saveIcon(body, file)
         compressIcon(file)
         userHelper.setIconPath(user, path)
      }
   }

   private fun saveIcon(body: ResponseBody, file: File) {
      body.let {
         it.byteStream().use { input ->
            file.outputStream().use { output ->
               input.copyTo(output)
            }
         }
      }
   }

   private fun compressIcon(file: File) {
      val sampleSize = getSampleSize(file)

      file.inputStream().use { inputStream ->
         val options = BitmapFactory.Options()
         options.inSampleSize = sampleSize
         val bitmap = BitmapFactory.decodeStream(inputStream, null, options)
         bitmap?.let {
            file.outputStream().use { outputStream ->
               it.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }
         }
      }
   }

   private fun getSampleSize(file: File): Int {
      val options = BitmapFactory.Options()
      options.inJustDecodeBounds = true
      return file.inputStream().use { inputStream ->
         BitmapFactory.decodeStream(inputStream, null, options)

         val height = options.outHeight
         val width = options.outWidth
         var inSampleSize = 1
         if (height > MAX_DIMENSION || width > MAX_DIMENSION) {
            // Calculate the largest inSampleSize value that is a power of 2 and will ensure
            // height and width is smaller than the max image we can process
            while (height / inSampleSize >= MAX_DIMENSION && height / inSampleSize >= MAX_DIMENSION) {
               inSampleSize *= 2
            }
         }

         inSampleSize
      }
   }

   companion object {
      private val LOG_NAME = UserRepository::class.java.name
      private const val MAX_DIMENSION = 200
   }
}