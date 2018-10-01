package mil.nga.giat.mage.dagger.module

import android.content.Context
import dagger.Module
import dagger.Provides
import mil.nga.giat.mage.MageApplication
import javax.inject.Singleton

@Module
class ContextModule {

    @Provides
    @Singleton
    fun context(application: MageApplication): Context {
        return application.applicationContext
    }
}
