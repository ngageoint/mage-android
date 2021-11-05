package mil.nga.giat.mage.di

import android.app.Application
import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import mil.nga.giat.mage.data.gson.AnnotationExclusionStrategy
import mil.nga.giat.mage.data.gson.DateTimestampTypeAdapter
import mil.nga.giat.mage.data.gson.GeometryTypeAdapterFactory
import mil.nga.giat.mage.network.LiveDataCallAdapterFactory
import mil.nga.giat.mage.network.Server
import mil.nga.giat.mage.network.api.*
import mil.nga.giat.mage.network.gson.LocationsTypeAdapter
import mil.nga.giat.mage.network.gson.observation.ObservationTypeAdapter
import mil.nga.giat.mage.network.gson.observation.ObservationsTypeAdapter
import mil.nga.giat.mage.network.gson.user.UserTypeAdapter
import mil.nga.giat.mage.sdk.datastore.layer.Layer
import mil.nga.giat.mage.sdk.datastore.location.Location
import mil.nga.giat.mage.sdk.datastore.observation.Observation
import mil.nga.giat.mage.sdk.datastore.user.Event
import mil.nga.giat.mage.sdk.datastore.user.Role
import mil.nga.giat.mage.sdk.datastore.user.Team
import mil.nga.giat.mage.sdk.datastore.user.User
import mil.nga.giat.mage.sdk.gson.deserializer.EventsDeserializer
import mil.nga.giat.mage.sdk.gson.deserializer.LayersDeserializer
import mil.nga.giat.mage.sdk.gson.deserializer.RolesDeserializer
import mil.nga.giat.mage.sdk.gson.deserializer.TeamsDeserializer
import mil.nga.giat.mage.sdk.http.HttpClientManager
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*
import javax.inject.Singleton


@InstallIn(SingletonComponent::class)
@Module
class NetworkModule {

   @Provides
   @Singleton
   fun provideHttpClient(): OkHttpClient {
      val builder = HttpClientManager.getInstance().httpClient().newBuilder()
      return builder.build()
   }

   @Provides
   @Singleton
   fun providerServer(@ApplicationContext context: Context): Server {
      return Server(context)
   }

   @Provides
   @Singleton
   fun provideGson(application: Application): Gson {
      return GsonBuilder()
         .setExclusionStrategies(AnnotationExclusionStrategy())
         .registerTypeAdapter(object : TypeToken<User>() {}.type, UserTypeAdapter(application))
         .registerTypeAdapter(object : TypeToken<Observation>() {}.type, ObservationTypeAdapter(application))
         .registerTypeAdapter(object : TypeToken<java.util.Collection<Role>>() {}.type, RolesDeserializer())
         .registerTypeAdapter(object : TypeToken<java.util.Collection<Layer>>() {}.type, LayersDeserializer())
         .registerTypeAdapter(object : TypeToken<java.util.List<Location>>() {}.type, LocationsTypeAdapter(application))
         .registerTypeAdapter(object : TypeToken<java.util.List<Observation>>() {}.type, ObservationsTypeAdapter(application))
         .registerTypeAdapter(object : TypeToken<java.util.Map<Team, java.util.Collection<User>>>() {}.type, TeamsDeserializer(application))
         .registerTypeAdapter(object : TypeToken<java.util.Map<Event, java.util.Collection<Team>>>() {}.type, EventsDeserializer(application))
         .registerTypeAdapterFactory(GeometryTypeAdapterFactory())
         .registerTypeAdapter(Date::class.java, DateTimestampTypeAdapter())
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
   fun provideObservationService(retrofit: Retrofit): ObservationService {
      return retrofit.create(ObservationService::class.java)
   }

   @Provides
   fun provideLocationService(retrofit: Retrofit): LocationService {
      return retrofit.create(LocationService::class.java)
   }

   @Provides
   fun provideFeedService(retrofit: Retrofit): FeedService {
      return retrofit.create(FeedService::class.java)
   }
}