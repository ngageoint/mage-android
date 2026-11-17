package mil.nga.giat.mage.di

import android.app.Application
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mil.nga.giat.mage.MageApplication
import javax.inject.Singleton

/**
 * Kept separate from ApplicationModule so instrumented tests can uninstall just this binding and
 * supply a fake SessionController via @BindValue, without losing ApplicationModule's other bindings.
 */
@InstallIn(SingletonComponent::class)
@Module
class SessionControllerModule {
    @Singleton
    @Provides
    internal fun provideSessionController(application: Application): SessionController {
        return application as MageApplication
    }
}
