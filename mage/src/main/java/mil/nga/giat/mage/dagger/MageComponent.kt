package mil.nga.giat.mage.dagger

import com.caci.kuato.di.module.ApplicationModule
import dagger.Component
import dagger.android.AndroidInjector
import dagger.android.support.AndroidSupportInjectionModule
import mil.nga.giat.mage.MageApplication
import mil.nga.giat.mage.dagger.contributor.ActivityContributorModule
import mil.nga.giat.mage.dagger.contributor.FragmentContributorModule
import mil.nga.giat.mage.dagger.contributor.ServiceContributorModule
import mil.nga.giat.mage.dagger.module.PreferencesModule
import javax.inject.Singleton

@Singleton
@Component(modules = arrayOf(
        AndroidSupportInjectionModule::class,
        ApplicationModule::class,
        PreferencesModule::class,
        ActivityContributorModule::class,
        FragmentContributorModule::class,
        ServiceContributorModule::class)
)
interface MageComponent : AndroidInjector<MageApplication> {

    @Component.Builder
    abstract class Builder : AndroidInjector.Builder<MageApplication>()

}