package mil.nga.giat.mage.di

import LocationsTypeAdapter
import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.mapbox.geojson.GeometryAdapterFactory
import com.mapbox.geojson.gson.GeoJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import mil.nga.giat.mage.network.gson.AnnotationExclusionStrategy
import mil.nga.giat.mage.network.gson.DateTimestampTypeAdapter
import mil.nga.giat.mage.network.geojson.GeometryTypeAdapterFactory
import mil.nga.giat.mage.network.LiveDataCallAdapterFactory
import mil.nga.giat.mage.network.Server
import mil.nga.giat.mage.network.api.*
import mil.nga.giat.mage.network.attachment.AttachmentTypeAdapter
import mil.nga.giat.mage.network.observation.ObservationTypeAdapter
import mil.nga.giat.mage.network.observation.ObservationsTypeAdapter
import mil.nga.giat.mage.database.model.layer.Layer
import mil.nga.giat.mage.network.attachment.AttachmentService
import mil.nga.giat.mage.network.attachment.AttachmentService_server5
import mil.nga.giat.mage.network.device.DeviceService
import mil.nga.giat.mage.network.event.EventService
import mil.nga.giat.mage.database.model.observation.Attachment
import mil.nga.giat.mage.database.model.observation.Observation
import mil.nga.giat.mage.database.model.event.Event
import mil.nga.giat.mage.database.model.location.Location
import mil.nga.giat.mage.database.model.permission.Role
import mil.nga.giat.mage.database.model.team.Team
import mil.nga.giat.mage.network.event.EventsDeserializer
import mil.nga.giat.mage.network.feed.FeedService
import mil.nga.giat.mage.network.geocoder.NominatimService
import mil.nga.giat.mage.network.layer.LayersDeserializer
import mil.nga.giat.mage.network.role.RolesDeserializer
import mil.nga.giat.mage.network.team.TeamsDeserializer
import mil.nga.giat.mage.network.layer.LayerService
import mil.nga.giat.mage.network.location.LocationService
import mil.nga.giat.mage.network.location.UserLocations
import mil.nga.giat.mage.network.location.UserLocationsTypeAdapter
import mil.nga.giat.mage.network.observation.ObservationService
import mil.nga.giat.mage.network.role.RoleService
import mil.nga.giat.mage.network.settings.SettingsService
import mil.nga.giat.mage.network.team.TeamService
import mil.nga.giat.mage.network.user.UserService
import mil.nga.giat.mage.network.user.UserWithRole
import mil.nga.giat.mage.network.user.UserWithRoleId
import mil.nga.giat.mage.network.user.UserWithRoleIdTypeAdapter
import mil.nga.giat.mage.network.user.UserWithRoleTypeAdapter
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.HttpURLConnection
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class Server5

data class UserAgentHeader(
   val name: String = "User-Agent",
   val value: String
)

@InstallIn(SingletonComponent::class)
@Module
class NetworkModule {

   @Singleton
   @Provides
   fun provideUserAgent(): UserAgentHeader {
      val value = System.getProperty("http.agent") ?: "Unknown Android Http Agent"
      return UserAgentHeader(value = value)
   }

   @Singleton
   @Provides
   fun provideHttpTokenInterceptor(
      tokenProvider: TokenProvider,
      userAgentHeader: UserAgentHeader
   ): Interceptor {
      val nonTokenRoutes = listOf("/auth/token", "/api/users/myself/password")
      return Interceptor { chain ->
         val builder = chain.request().newBuilder()

         if (!nonTokenRoutes.contains(chain.request().url.encodedPath)) {
            tokenProvider.value?.let { tokenStatus ->
               if (tokenStatus is TokenStatus.Active) {
                  builder.addHeader("Authorization", "Bearer ${tokenStatus.token.token}")
               }
            }
         }

         builder.addHeader(userAgentHeader.name, userAgentHeader.value)
         val response = chain.proceed(builder.build())
         val statusCode = response.code
         if (statusCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
            Log.d(LOG_NAME, "Token expired")
            tokenProvider.expireToken()
         } else if (statusCode == HttpURLConnection.HTTP_NOT_FOUND) {
            Log.w(LOG_NAME, response.message)
         }
         response
      }
   }

   @Singleton
   @Provides
   fun provideOkHttpClient(
      tokenInterceptor: Interceptor
   ): OkHttpClient {
      return OkHttpClient.Builder()
         .connectTimeout(60, TimeUnit.SECONDS)
         .readTimeout(60, TimeUnit.SECONDS)
         .writeTimeout(60, TimeUnit.SECONDS)
         .addInterceptor(tokenInterceptor)
         .build()
   }

   @Provides
   @Singleton
   fun providerServer(@ApplicationContext context: Context): Server {
      return Server(context)
   }

   @Provides
   @Singleton
   fun provideGson(): Gson {
      return GsonBuilder()
         .setLenient()
         .setExclusionStrategies(AnnotationExclusionStrategy())
         .registerTypeAdapter(object : TypeToken<UserWithRole>() {}.type, UserWithRoleTypeAdapter())
         .registerTypeAdapter(object : TypeToken<UserWithRoleId>() {}.type, UserWithRoleIdTypeAdapter())
         .registerTypeAdapter(object : TypeToken<Observation>() {}.type, ObservationTypeAdapter())
         .registerTypeAdapter(object : TypeToken<Attachment>() {}.type, AttachmentTypeAdapter())
         .registerTypeAdapter(object : TypeToken<java.util.List<Role>>() {}.type, RolesDeserializer())
         .registerTypeAdapter(object : TypeToken<java.util.List<Layer>>() {}.type, LayersDeserializer())
         .registerTypeAdapter(object : TypeToken<java.util.List<Location>>() {}.type, LocationsTypeAdapter())
         .registerTypeAdapter(object : TypeToken<java.util.List<UserLocations>>() {}.type, UserLocationsTypeAdapter())
         .registerTypeAdapter(object : TypeToken<java.util.List<Observation>>() {}.type, ObservationsTypeAdapter())
         .registerTypeAdapter(object : TypeToken<java.util.Map<Team, java.util.List<UserWithRoleId>>>() {}.type, TeamsDeserializer())
         .registerTypeAdapter(object : TypeToken<java.util.List<Event>>() {}.type, EventsDeserializer())
         .registerTypeAdapter(Date::class.java, DateTimestampTypeAdapter())
         .registerTypeAdapterFactory(GeoJsonAdapterFactory.create())
         .registerTypeAdapterFactory(GeometryAdapterFactory.create())
         .registerTypeAdapterFactory(GeometryTypeAdapterFactory())
         .create()
   }

   @Provides
   fun provideRetrofit(
      gson: Gson,
      okHttpClient: OkHttpClient,
      server: Server
   ): Retrofit {
      return Retrofit.Builder()
         .addConverterFactory(GsonConverterFactory.create(gson))
         .addCallAdapterFactory(LiveDataCallAdapterFactory())
         .baseUrl(server.baseUrl)
         .client(okHttpClient)
         .build()
   }

   @Provides
   fun provideApiService(retrofit: Retrofit): ApiService {
      return retrofit.create(ApiService::class.java)
   }

   @Provides
   fun provideSettingsService(retrofit: Retrofit): SettingsService {
      return retrofit.create(SettingsService::class.java)
   }

   @Provides
   fun provideNominatimService(retrofit: Retrofit): NominatimService {
      return retrofit.create(NominatimService::class.java)
   }

   @Provides
   fun provideRoleService(retrofit: Retrofit): RoleService {
      return retrofit.create(RoleService::class.java)
   }

   @Provides
   fun provideEventService(retrofit: Retrofit): EventService {
      return retrofit.create(EventService::class.java)
   }

   @Provides
   fun provideLayerService(retrofit: Retrofit): LayerService {
      return retrofit.create(LayerService::class.java)
   }

   @Provides
   fun provideTeamService(retrofit: Retrofit): TeamService {
      return retrofit.create(TeamService::class.java)
   }

   @Provides
   fun provideUserService(retrofit: Retrofit): UserService {
      return retrofit.create(UserService::class.java)
   }

   @Provides
   fun provideDeviceService(retrofit: Retrofit): DeviceService {
      return retrofit.create(DeviceService::class.java)
   }

   @Provides
   fun provideObservationService(retrofit: Retrofit): ObservationService {
      return retrofit.create(ObservationService::class.java)
   }

   @Provides
   fun provideAttachmentService(retrofit: Retrofit): AttachmentService {
      return retrofit.create(AttachmentService::class.java)
   }

   @Provides
   @Server5
   fun provideAttachmentService_server5(retrofit: Retrofit): AttachmentService_server5 {
      return retrofit.create(AttachmentService_server5::class.java)
   }

   @Provides
   fun provideLocationService(retrofit: Retrofit): LocationService {
      return retrofit.create(LocationService::class.java)
   }

   @Provides
   fun provideFeedService(retrofit: Retrofit): FeedService {
      return retrofit.create(FeedService::class.java)
   }

   companion object {
      private val LOG_NAME = NetworkModule::class.java.name
   }
}