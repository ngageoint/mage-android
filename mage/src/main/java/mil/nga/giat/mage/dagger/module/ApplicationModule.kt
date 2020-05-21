package mil.nga.giat.mage.dagger.module

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.android.support.AndroidSupportInjectionModule
import mil.nga.giat.mage.MageApplication
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
annotation class ApplicationContext

@Module(includes = [AndroidSupportInjectionModule::class, ViewModelModule::class])
abstract class ApplicationModule {

    @Singleton
    @Binds
    @ApplicationContext
    abstract fun provideContext(application: MageApplication): Context

}
