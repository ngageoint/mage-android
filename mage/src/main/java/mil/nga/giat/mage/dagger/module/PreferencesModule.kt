package mil.nga.giat.mage.dagger.module

import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mil.nga.giat.mage.MageApplication
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class PreferencesModule {

    @Provides
    @Singleton
    internal fun providePreferences(application: MageApplication): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(application)
    }
}
