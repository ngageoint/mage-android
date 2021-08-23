package mil.nga.giat.mage.dagger

import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.android.AndroidInjector
import dagger.android.support.AndroidSupportInjectionModule
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mil.nga.giat.mage.MageApplication
import mil.nga.giat.mage.dagger.contributor.ActivityContributorModule
import mil.nga.giat.mage.dagger.contributor.FragmentContributorModule
import mil.nga.giat.mage.dagger.contributor.ServiceContributorModule
import mil.nga.giat.mage.dagger.module.ApplicationModule
import mil.nga.giat.mage.dagger.module.PreferencesModule
import javax.inject.Singleton

//@Singleton
//@Component(modules = [
////    AndroidSupportInjectionModule::class,
////    ApplicationModule::class,
////    PreferencesModule::class,
////    ActivityContributorModule::class,
////    FragmentContributorModule::class,
////    ServiceContributorModule::class
//])
//interface MageComponent {
//
////    @Component.Factory
////    interface Factory {
////        fun create(@BindsInstance application: MageApplication): MageComponent
////    }
//}

// Becomes the following classes
@InstallIn(SingletonComponent::class)
@Module(includes = [
    AndroidSupportInjectionModule::class,
    ApplicationModule::class,
    PreferencesModule::class,
    ActivityContributorModule::class,
    FragmentContributorModule::class,
    ServiceContributorModule::class
])
interface AggregatorModule {}