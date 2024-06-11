package mil.nga.giat.mage.di

import android.app.Application
import android.content.Context
import androidx.annotation.NonNull
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import mil.nga.giat.mage.MageApplication
import javax.inject.Singleton


@InstallIn(SingletonComponent::class)
@Module
class ApplicationModule {
    private lateinit var application: Application

    @Singleton
    @Provides
    internal fun provideApplication(application: Application): MageApplication {
        this.application = application
        return application as MageApplication
    }

    @Singleton
    @Provides
//    @ApplicationContext
    fun provideContext(): Context {
        return application
    }
}
