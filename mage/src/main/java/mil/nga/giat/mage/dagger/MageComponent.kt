package mil.nga.giat.mage.dagger

import dagger.BindsInstance
import dagger.Component
import dagger.android.AndroidInjector
import dagger.android.support.AndroidSupportInjectionModule
import mil.nga.giat.mage.MageApplication
import mil.nga.giat.mage.dagger.contributor.ActivityContributorModule
import mil.nga.giat.mage.dagger.contributor.FragmentContributorModule
import mil.nga.giat.mage.dagger.contributor.ServiceContributorModule
import mil.nga.giat.mage.dagger.module.ApplicationModule
import mil.nga.giat.mage.dagger.module.NetworkModule
import mil.nga.giat.mage.dagger.module.PreferencesModule
import mil.nga.giat.mage.dagger.module.RoomModule
import javax.inject.Singleton

@Singleton
@Component(modules = [
    AndroidSupportInjectionModule::class,
    ApplicationModule::class,
    PreferencesModule::class,
    RoomModule::class,
    NetworkModule::class,
    ActivityContributorModule::class,
    FragmentContributorModule::class,
    ServiceContributorModule::class
])
interface MageComponent : AndroidInjector<MageApplication> {

    @Component.Factory
    interface Factory {
        fun create(@BindsInstance application: MageApplication): MageComponent
    }
}