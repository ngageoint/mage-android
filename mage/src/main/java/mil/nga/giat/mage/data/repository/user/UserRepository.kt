package mil.nga.giat.mage.data.repository.user

import android.app.Application
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.stream.JsonReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mil.nga.giat.mage.R
import mil.nga.giat.mage.data.datasource.permission.RoleLocalDataSource
import mil.nga.giat.mage.di.TokenProvider
import mil.nga.giat.mage.login.AuthenticationStatus
import mil.nga.giat.mage.login.AuthorizationStatus
import mil.nga.giat.mage.network.device.DeviceService
import mil.nga.giat.mage.network.user.UserService
import mil.nga.giat.mage.sdk.Compatibility.Companion.isCompatibleWith
import mil.nga.giat.mage.database.model.event.Event
import mil.nga.giat.mage.database.model.user.User
import mil.nga.giat.mage.data.datasource.user.UserLocalDataSource
import mil.nga.giat.mage.database.dao.MageSqliteOpenHelper
import mil.nga.giat.mage.network.user.UserWithRoleTypeAdapter
import mil.nga.giat.mage.sdk.preferences.PreferenceHelper
import mil.nga.giat.mage.sdk.utils.DeviceUuidFactory
import mil.nga.giat.mage.sdk.utils.ISO8601DateFormatFactory
import mil.nga.giat.mage.sdk.utils.MediaUtility
import mil.nga.giat.mage.sdk.utils.PasswordUtility
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import java.io.File
import java.io.StringReader
import java.util.Base64
import java.util.Date
import javax.inject.Inject

class UserRepository @Inject constructor(
   private val application: Application,
   private val preferences: SharedPreferences,
   private val daoStore: MageSqliteOpenHelper,
   private val userService: UserService,
   private val deviceService: DeviceService,
   private val tokenProvider: TokenProvider,
   private val roleLocalDataSource: RoleLocalDataSource,
   private val userLocalDataSource: UserLocalDataSource
) {

   suspend fun authenticateLocal(strategy: String, username: String, password: String): AuthenticationStatus {
      val parameters = JsonObject()
      parameters.addProperty("username", username)
      parameters.addProperty("password", password)

      try {
         val packageInfo = application.packageManager.getPackageInfo(application.packageName, 0)
         parameters.addProperty(
            "appVersion",
            String.format("%s-%s", packageInfo.versionName, packageInfo.versionCode)
         )
      } catch (e: PackageManager.NameNotFoundException) {
         Log.w(LOG_NAME, "Problem sending package info on local signin", e)
      }

      val status = try {
         val response = userService.signin(strategy, parameters)
         if (response.isSuccessful) {
            val json = response.body()!!
            AuthenticationStatus.Success(
               username = username,
               token = json["token"].asString
            )
         } else if (response.code() == 403) {
            val message = response.errorBody()?.string() ?: "User account is not approved, please contact your MAGE administrator to approve your account."
            AuthenticationStatus.AccountCreated(message = message)
         } else {
            val message = response.errorBody()?.string() ?: "Please check your username and password and try again."
            AuthenticationStatus.Failure(
               code = response.code(),
               message = message
            )
         }
      } catch (e: Exception) {
         val message = "Error connecting to server, please contact your MAGE administrator"
         val offlineUsername = preferences.getString(application.getString(R.string.usernameKey), null)
         val offlinePassword = preferences.getString(application.getString(R.string.passwordHashKey), null)
         if (offlineUsername == username && PasswordUtility.equal(password, offlinePassword)) {
            AuthenticationStatus.Offline(message)
         } else {
            AuthenticationStatus.Failure(
               code = 500,
               message = message
            )
         }
      }

      return status
   }

   fun authenticateOffline() {
      val tokenExpirationLength = preferences.getLong(application.getString(R.string.tokenExpirationLengthKey), 0).coerceAtLeast(0)
      val tokenExpiration = Date(System.currentTimeMillis() + tokenExpirationLength)
      preferences.edit().putString(
         application.getString(R.string.tokenExpirationDateKey),
         ISO8601DateFormatFactory.ISO8601().format(tokenExpiration)
      ).apply()
   }

   suspend fun authorize(strategy: String, jwt: String): AuthorizationStatus {
      val parameters = JsonObject()
      parameters.addProperty("uid", DeviceUuidFactory(application).deviceUuid.toString())

      val (_, payload) = jwt.split(".")
      val json = String(Base64.getDecoder().decode(payload))
      val authorizedUser = Gson().fromJson(json, JsonObject::class.java)["sub"].asString?.let { subject ->
         userLocalDataSource.read(subject)
      }

      return try {
         val authorizeResponse = deviceService.authorize(
            String.format("Bearer %s", jwt),
            System.getProperty("http.agent"),
            parameters
         )

         if (authorizeResponse.isSuccessful) {
            val authorization = authorizeResponse.body()

            // Check server api version to ensure compatibility before continuing
            val serverVersion = authorization!!["api"].asJsonObject["version"].asJsonObject
            if (!isCompatibleWith(serverVersion["major"].asInt, serverVersion["minor"].asInt)) {
               Log.e(LOG_NAME, "Server version not compatible")
               AuthorizationStatus.FailInvalidServer
            }

            // Successful login, put the token information in the shared preferences
            val token = authorization["token"].asString
            val tokenExpiration = authorization["expirationDate"].asString.trim()
            val userJson = authorization.getAsJsonObject("user")
            val reader = JsonReader(StringReader(userJson.toString()))
            val userWithRole = UserWithRoleTypeAdapter().read(reader)

            val previousUser = preferences.getString(application.getString(R.string.sessionUserKey), null)
            val previousStrategy = preferences.getString(application.getString(R.string.sessionStrategyKey), null)
            val sessionChanged =
               (previousStrategy != null && strategy != previousStrategy) ||
               (previousUser != null && userWithRole.user.username != previousUser)

            if (sessionChanged) {
               daoStore.resetDatabase()

               val preferenceHelper = PreferenceHelper.getInstance(application)
               preferenceHelper.initialize(true, R.xml::class.java)

               val dayNightTheme = preferences.getInt(application.resources.getString(R.string.dayNightThemeKey), application.resources.getInteger(R.integer.dayNightThemeDefaultValue))
               AppCompatDelegate.setDefaultNightMode(dayNightTheme)
            }

            roleLocalDataSource.read(userWithRole.role.remoteId)?.let { userWithRole.role.id = it.id }

            val role = roleLocalDataSource.createOrUpdate(userWithRole.role)
            userWithRole.user.role = role
            userWithRole.user.fetchedDate = Date()
            val user = userLocalDataSource.createOrUpdate(userWithRole.user)
            userLocalDataSource.setCurrentUser(user)

            tokenProvider.updateToken(
               username = user.username,
               authenticationStrategy = strategy,
               token = token.trim(),
               expiration = tokenExpiration
            )

            AuthorizationStatus.Success(
               user = user,
               sessionChanged = sessionChanged
            )
         } else {
            val code = authorizeResponse.code()
            if (code == 403) {
               AuthorizationStatus.FailAuthorization(user = authorizedUser)
            } else {
               AuthorizationStatus.FailAuthentication(
                  user = authorizedUser,
                  message = "Authorization error, please contact your MAGE administrator for assistance"
               )
            }
         }
      } catch (e: Exception) {
         Log.e(LOG_NAME, "Error authorizing device", e)
         AuthorizationStatus.FailAuthorization()
      }
   }

   fun signout() {
      try {
         userService.signout()
         tokenProvider.signout()
      } catch (e: Exception) {
         Log.e(LOG_NAME, "Error signing out", e)
      }
   }

   suspend fun getCaptcha(username: String, background: String): Response<JsonObject> {
      val json = JsonObject()
      json.addProperty("username", username)
      json.addProperty("background", background)
      return userService.signup(json)
   }

   suspend fun verifyUser(
      displayName: String?,
      email: String?,
      phone: String?,
      password: String?,
      captchaText: String?,
      token: String?
   ): Response<JsonObject> {
      val json = JsonObject()
      json.addProperty("displayName", displayName)
      json.addProperty("email", email)
      json.addProperty("phone", phone)
      json.addProperty("password", password)
      json.addProperty("passwordconfirm", password)
      json.addProperty("captchaText", captchaText)
      return userService.signupVerify(String.format("Bearer %s", token), json)
   }

   suspend fun fetchUsers(ids: List<String>) {
      ids.filter { it != "-1" }.forEach { id ->
         try {
            val response = userService.getUser(id)
            if (response.isSuccessful) {
               response.body()?.let { (user, role) ->
                  roleLocalDataSource.read(role.remoteId).let { user.role = it }
                  user.fetchedDate = Date()
                  roleLocalDataSource.createOrUpdate(role)
                  userLocalDataSource.createOrUpdate(user)
               }
            }
         } catch (e: Exception) {
            Log.e(LOG_NAME, "Error fetching user", e)
         }
      }
   }

   suspend fun changePassword(
      username: String?,
      password: String?,
      newPassword: String?,
      newPasswordConfirm: String?
   ): Response<JsonObject> {
      val json = JsonObject()
      json.addProperty("username", username)
      json.addProperty("password", password)
      json.addProperty("newPassword", newPassword)
      json.addProperty("newPasswordConfirm", newPasswordConfirm)
      return userService.changePassword(json)
   }

   suspend fun syncAvatar(path: String) = withContext(Dispatchers.IO) {
      Log.i(LOG_NAME, "Pushing user avatar $path")

      try {
         val parts: MutableMap<String, RequestBody> = HashMap()
         val avatar = File(path)
         val mimeType = MediaUtility.getMimeType(path)
         val fileBody = RequestBody.create(mimeType.toMediaTypeOrNull(), avatar)
         parts["avatar\"; filename=\"" + avatar.name + "\""] = fileBody
         val response = userService.createAvatar(parts)
         if (response.isSuccessful) {
            response.body()?.let { (user, role) ->
               roleLocalDataSource.createOrUpdate(role)
               userLocalDataSource.readCurrentUser()?.let { currentUser ->
                  currentUser.avatarUrl = user.avatarUrl
                  currentUser.lastModified = Date(currentUser.lastModified.time + 1)
                  userLocalDataSource.update(currentUser)
                  userLocalDataSource.setAvatarPath(currentUser, null)
                  Log.d(LOG_NAME, "Updated user with remote_id " + user.remoteId)
               }
            }
         } else {
            Log.e(LOG_NAME, "Bad request.")
            response.errorBody()?.let { error ->
               Log.e(LOG_NAME, error.string())
            }
         }
      } catch (e: java.lang.Exception) {
         Log.e(LOG_NAME, "Failure saving observation.", e)
      }
   }

   suspend fun syncIcons(event: Event) = withContext(Dispatchers.IO) {
      val users = userLocalDataSource.getUsersInEvent(event)
      for (user in users) {
         try {
            syncIcon(user)
         } catch (e: Exception) {
            Log.e(LOG_NAME, "Error syncing user icon", e)
         }
      }
   }

   private suspend fun syncIcon(user: User) {
      val path = "${MediaUtility.getUserIconDirectory(application)}/${user.id}.png"
      val file = File(path)
      if (file.exists() && user.fetchedDate.before(user.lastModified)) {
         file.delete()
         val response = userService.getIcon(user.remoteId)
         val body = response.body()
         if (response.isSuccessful && body != null) {
            saveIcon(body, file)
            compressIcon(file)
            userLocalDataSource.setIconPath(user, path)
         }
      } else {
         userLocalDataSource.setIconPath(user, path)
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