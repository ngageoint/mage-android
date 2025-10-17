package mil.nga.giat.mage.di

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mil.nga.giat.mage.MageApplication
import mil.nga.giat.mage.utils.USER_FILTER_PREFS_FILE
import javax.inject.Qualifier
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class PreferencesModule {

    @Provides
    @Singleton
    internal fun providePreferences(application: MageApplication): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(application)
    }

    @Target(
        AnnotationTarget.PROPERTY,
        AnnotationTarget.VALUE_PARAMETER,
        AnnotationTarget.FUNCTION,
        AnnotationTarget.TYPE
    )
    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class UserFilterPreferences

    @Provides
    @Singleton
    @UserFilterPreferences
    internal fun provideUserFilterPreferences(application: MageApplication): SharedPreferences {
        return application.getSharedPreferences(USER_FILTER_PREFS_FILE, Context.MODE_PRIVATE)
    }
}
