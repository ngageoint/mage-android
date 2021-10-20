package mil.nga.giat.mage.di

import android.app.Application
import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
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
import mil.nga.giat.mage.sdk.gson.deserializer.TeamsDeserializer
import mil.nga.giat.mage.sdk.http.HttpClientManager
import mil.nga.giat.mage.sdk.http.resource.EventResource
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
        val builder =  HttpClientManager.getInstance().httpClient().newBuilder()
        return builder.build()
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
            .setExclusionStrategies(AnnotationExclusionStrategy())
            .registerTypeAdapterFactory(GeometryTypeAdapterFactory())
            .registerTypeAdapter(Date::class.java, DateTimestampTypeAdapter())
            .create()
    }

    @Provides
    fun provideRetrofit(gson: Gson, okHttpClient: OkHttpClient, server: Server, application: Application): Retrofit {
        return Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create(TeamsDeserializer.getGsonBuilder(application)))
            .addConverterFactory(GsonConverterFactory.create(gson))
            .addCallAdapterFactory(LiveDataCallAdapterFactory())
            .baseUrl(server.baseUrl)
            .client(okHttpClient)
            .build()
    }

    @Provides
    fun provideEventService(retrofit: Retrofit): EventResource.EventService {
        return retrofit.create(EventResource.EventService::class.java)
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
    fun provideFeedService(retrofit: Retrofit): FeedService {
        return retrofit.create(FeedService::class.java)
    }
}