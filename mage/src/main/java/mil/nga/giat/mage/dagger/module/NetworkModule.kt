package mil.nga.giat.mage.dagger.module

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import dagger.Module
import dagger.Provides
import mil.nga.giat.mage.data.gson.AnnotationExclusionStrategy
import mil.nga.giat.mage.data.gson.DateTimestampTypeAdapter
import mil.nga.giat.mage.data.gson.GeometryTypeAdapterFactory
import mil.nga.giat.mage.network.LiveDataCallAdapterFactory
import mil.nga.giat.mage.network.Server
import mil.nga.giat.mage.network.api.FeedService
import mil.nga.giat.mage.sdk.http.HttpClientManager
import mil.nga.giat.mage.sdk.http.resource.EventResource
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*
import javax.inject.Singleton

@Module
class NetworkModule {

    @Provides
    @Singleton
    fun provideHttpClient(@ApplicationContext context: Context): OkHttpClient {
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
    fun provideRetrofit(gson: Gson, okHttpClient: OkHttpClient, server: Server): Retrofit {
        return Retrofit.Builder()
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
    fun provideFeedService(retrofit: Retrofit): FeedService {
        return retrofit.create(FeedService::class.java)
    }
}