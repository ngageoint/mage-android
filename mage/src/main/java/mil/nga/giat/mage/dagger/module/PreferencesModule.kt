package mil.nga.giat.mage.dagger.module

import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import dagger.Module
import dagger.Provides
import mil.nga.giat.mage.MageApplication
import javax.inject.Singleton

@Module
class PreferencesModule {

    @Provides
    @Singleton
    internal fun providePreferences(application: MageApplication): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(application)
    }
}
